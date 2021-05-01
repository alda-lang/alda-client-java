package alda.repl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jline.console.ConsoleReader;
import jline.console.history.FileHistory;
import jline.console.UserInterruptException;

import org.fusesource.jansi.AnsiConsole;
import org.fusesource.jansi.Ansi.Color;
import static org.fusesource.jansi.Ansi.*;
import static org.fusesource.jansi.Ansi.Color.*;

import alda.AldaServer;
import alda.AldaResponse;
import alda.AldaResponse.AldaScore;
import alda.error.AlreadyUpException;
import alda.error.ExitCode;
import alda.error.InvalidOptionsException;
import alda.error.NoAvailableWorkerException;
import alda.error.NoResponseException;
import alda.error.SystemException;
import alda.error.UnsuccessfulException;
import alda.Util;

import alda.repl.commands.ReplCommand;
import alda.repl.commands.ReplCommandManager;

public class AldaRepl {
  public static final String ASCII_ART  =
    " █████╗ ██╗     ██████╗  █████╗\n" +
    "██╔══██╗██║     ██╔══██╗██╔══██╗\n" +
    "███████║██║     ██║  ██║███████║\n" +
    "██╔══██║██║     ██║  ██║██╔══██║\n" +
    "██║  ██║███████╗██████╔╝██║  ██║\n" +
    "╚═╝  ╚═╝╚══════╝╚═════╝ ╚═╝  ╚═╝\n";

  public static final int ASCII_WIDTH = ASCII_ART.substring(0, ASCII_ART.indexOf('\n')).length();
  public static final String HELP_TEXT = "Type :help for a list of available commands.";
  public static final String PROMPT = "> ";

  public static final int DEFAULT_NUMBER_OF_WORKERS = 2;

  private AldaServer server;
  private ConsoleReader r;
  private boolean verbose;

  private ReplCommandManager manager;

  private StringBuffer history;
  private FileHistory fileHistory;

  private String promptPrefix = "";

  public AldaRepl(AldaServer server, boolean verbose) {
    this.server = server;
    server.setQuiet(true);
    this.verbose = verbose;
    history = new StringBuffer();
    manager = new ReplCommandManager();
    try {
      r = new ConsoleReader();

      // Capture Ctrl-C and throw a jline.console.UserInterruptException.
      r.setHandleUserInterrupt(true);

      // Disable default behavior where JLine treats `!` like a Bash expansion
      // character.
      r.setExpandEvents(false);

      // Enable history file.
      Path historyFile = Paths.get(System.getProperty("user.home"),
                                   ".alda-repl-history");
      if (!historyFile.toFile().exists()) Files.createFile(historyFile);
      fileHistory = new FileHistory(historyFile.toFile());
      r.setHistory(fileHistory);
      r.setHistoryEnabled(true);
    } catch (IOException e) {
      System.err.println("An error was detected when we tried to read a line.");
      e.printStackTrace();
      ExitCode.SYSTEM_ERROR.exit();
    }
    if (!server.noColor) AnsiConsole.systemInstall();
  }

  /**
   * Centers and colors text
   * @param totalLen the total length to center to
   * @param toFormat the string to format
   * @param color the ANSI code to color. null will result in no color
   */
  private String centerText(int totalLen, String toFormat, Color color) {
    int offset = totalLen / 2 - toFormat.length() / 2;
    String out = "";
    if (offset > 0) {
      // Print out spaces to center the version string
      out = out + String.format("%1$"+offset+"s", " ");
    }
    out = out + toFormat;
    if (!server.noColor && color != null) {
      out = ansi().fg(color).a(out).reset().toString();
    }
    return out;
  }

  /**
   * Sanitizes instruments to remove their inst id.
   * guitar-IrqxY becomes guitar
   */
  public String sanitizeInstrument(String instrument) {
    return instrument.replaceFirst("-\\w+$", "");
  }

  /**
   * Converts the current instrument to it's prefix form
   * For example, midi-square-wave becomes msw
   */
  public String instrumentToPrefix(String instrument) {
    // Split on non-words
    String[] parts = instrument.split("\\W");
    StringBuffer completedName = new StringBuffer();
    // Build new name on first char of every part from the above split.
    for (String s : parts) {
      if (s.length() > 0)
        completedName.append(s.charAt(0));
    }
    return completedName.toString();
  }

  public void setPromptPrefix(AldaScore score) {
    if (score != null
		&& score.currentInstruments() != null
		&& score.currentInstruments().size() > 0) {
	  Set<String> instruments = score.currentInstruments();
	  boolean nicknameFound = false;
	  String newPrompt = null;
      if (score.nicknames != null) {
		// Convert nick -> inst map to inst -> nick
		for (Map.Entry<String, Set<String>> entry : score.nicknames.entrySet()) {
		  // Check to see if we are playing any instruments from the nickname value set.
		  Set<String> val = entry.getValue();
		  // This destroys the nicknames value sets in the process.
		  val.retainAll(instruments);
		  if (val.size() > 0) {
			// Remove a possible period seperator, IE: nickname.piano
			newPrompt = entry.getKey().replaceFirst("\\.\\w+$", "");
			newPrompt = instrumentToPrefix(newPrompt);
			break;
		  }
		}
	  }

	  // No groups found, translate instruments normally:
	  if (newPrompt == null) {
		newPrompt =
		  score.currentInstruments().stream()
		  // Translate instruments to nicknames if available
		  .map(this::sanitizeInstrument)
		  // Translate midi-electric-piano-1 -> mep1
		  .map(this::instrumentToPrefix)
		  // Combine all instruments with /
		  .reduce("", (a, b) -> a + "/" + b)
		  // remove leading / (which is always present)
		  .substring(1);
	  }

	  if (newPrompt != null && newPrompt.length() > 0) {
        promptPrefix = newPrompt;
        return;
      }
    }
    // If we failed anywhere, reset prompt (probably no instruments playing).
    promptPrefix = "";
  }

  // Used by the Alda REPL.
  //
  // Errors are handled internally; if one occurs, the stacktrace is printed and
  // execution continues.
  private void offerToStartServer() {
    System.out.println("The server is down. Start server on port " +
                       server.port + "?");
    try {
      switch (Util.promptWithChoices(r, Arrays.asList("yes", "no", "quit"))) {
        case "yes":
          try {
            System.out.println();
            server.setQuiet(false);
            server.upBg(DEFAULT_NUMBER_OF_WORKERS);
            server.setQuiet(true);
          } catch (InvalidOptionsException | NoResponseException |
                   AlreadyUpException | SystemException e) {
            System.err.println("Unable to start server:");
            e.printStackTrace();
          }
          break;
        case "no":
          // do nothing
          break;
        case "quit":
          ExitCode.SUCCESS.exit();
        default:
          // this shouldn't happen; if it does, just move on
          break;
      }
    } catch (SystemException e) {
      System.err.println("Error trying to read character:");
      e.printStackTrace();
    }
    System.out.println();
  }

  private String asciiArt() {
    if (server.noColor) return ASCII_ART;
    return ansi().fg(BLUE).a(ASCII_ART).reset().toString();
  }

  private String helpText() {
    if (server.noColor) return HELP_TEXT;
    return ansi().fg(WHITE).bold().a(HELP_TEXT).reset().toString();
  }

  public void run() {
    System.out.println(asciiArt());
    System.out.println(centerText(ASCII_WIDTH, Util.version(), CYAN));
    System.out.println(centerText(ASCII_WIDTH, "repl session", CYAN));
    System.out.println("\n" + helpText() + "\n");

    if (!server.checkForConnection()) {
      offerToStartServer();
    }

    while (true) {
      String input = "";
      try {
        input = r.readLine(promptPrefix + PROMPT);
        fileHistory.flush();
      } catch (IOException e) {
        System.err.println("An error was detected when we tried to read a line.");
        e.printStackTrace();
        ExitCode.SYSTEM_ERROR.exit();
      } catch (UserInterruptException e) {
		input = ":quit";
	  }

      // Check for quick quit keywords. input is null when we get EOF
      if (input == null || input.matches("^:?(quit|exit|bye).*")) {
        // If we got an EOF, we need to print a line, so we quit on a newline
        if (input == null)
          System.out.println();

        // Let the master quit function handle this.
        input = ":quit";
      }

      if (input.length() == 0) {
        // Don't do anything if we get no input
        continue;
      }

      // check for :keywords and act on them
      if (input.charAt(0) == ':') {
        // throw away ':'
        input = input.substring(1);

        // This limits size of splitString to 2 elements.
        // All arguments will be in splitString[1]
        String[] splitString = input.split("\\s", 2);
        ReplCommand cmd = manager.get(splitString[0]);

        if (cmd != null) {
          // pass in empty string if we have no arguments
          String arguments = splitString.length > 1 ? splitString[1] : "";
          // Run the command
          try {
            cmd.act(arguments.trim(), history, server, r, this::setPromptPrefix);
          } catch (NoResponseException e) {
            System.out.println();
            offerToStartServer();
          } catch (UserInterruptException e) {
            try {
              // Quit the repl
              cmd.act(":quit", history, server, r, this::setPromptPrefix);
            } catch (NoResponseException nre) {
              // This shouldn't happen, but if it does...
              nre.printStackTrace();
              // Intentionally quitting should be considered successful.
              ExitCode.SUCCESS.exit();
            }
          }
        } else {
          System.err.println("No command '" + splitString[0] + "' was found");
        }
      } else {
        try {
          // Play the stuff we just got, with history as context
          AldaResponse playResponse = server.playFromRepl(
            input, history.toString(), null, null
          );

          // If we have no exceptions, add to history
          history.append(input);
          history.append("\n");

          // If we're good, we should check to see if we reset the instrument
          if (playResponse != null) {
            this.setPromptPrefix(playResponse.score);
          }
        } catch (NoResponseException e) {
          System.out.println();
          offerToStartServer();
        } catch (NoAvailableWorkerException | UnsuccessfulException e) {
          server.error(e.getMessage());
          if (verbose) {
            System.out.println();
            e.printStackTrace();
          }
        }
      }
    }
  }
}
