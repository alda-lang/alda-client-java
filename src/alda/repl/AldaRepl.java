
package alda.repl;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jline.console.ConsoleReader;
import jline.console.completer.Completer;

import org.fusesource.jansi.AnsiConsole;
import org.fusesource.jansi.Ansi.Color;
import static org.fusesource.jansi.Ansi.*;
import static org.fusesource.jansi.Ansi.Color.*;

import alda.AldaServer;
import alda.AldaClient;

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

  private AldaServer server;
  private ConsoleReader r;
  private boolean verbose;

  private ReplCommandManager manager;

  private StringBuffer history;

  public AldaRepl(AldaServer server, boolean verbose) {
    this.server = server;
    server.setQuiet(true);
    this.verbose = verbose;
    history = new StringBuffer();
    manager = new ReplCommandManager();
    try {
      r = new ConsoleReader();
    } catch (IOException e) {
      System.err.println("An error was detected when we tried to read a line.");
      e.printStackTrace();
      System.exit(1);
    }
    AnsiConsole.systemInstall();
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
    if (color != null) {
      out = ansi().fg(color).a(out).reset().toString();
    }
    return out;
  }

  private static final Pattern promptPattern = Pattern.compile("[-a-z0-10]+:");
  /**
   * Generates the prompt prefix given a history/code buffer
   * @param in The string to check for instrument: directives
   * @param defaultStr The default string to return if we can't find any matches
   */
  private String genPromptPrefix(CharSequence in, CharSequence defaultStr) {
    if (defaultStr == null)
      defaultStr = "";

    if (in == null || in.length() == 0) {
      return defaultStr.toString();
    }
    Matcher result = promptPattern.matcher(in);
    String match = null;

    while (result.find()) {
      match = result.group();
    }
    if (match != null) {
      return match.charAt(0) + "";
    }
    return defaultStr.toString();
  }

  public void run() {
    System.out.println(ansi().fg(BLUE).a(ASCII_ART).reset());
    System.out.println(centerText(ASCII_WIDTH, AldaClient.version(), CYAN));
    System.out.println(centerText(ASCII_WIDTH, "repl session", CYAN));

    System.out.println("\n" + ansi().fg(WHITE).bold().a(HELP_TEXT).reset() + "\n");

    String promptPrefix = genPromptPrefix(null, null);

    while (true) {
      String input = "";
      try {
        // TODO add dynamic prompts based on the instrument
        input = r.readLine(promptPrefix + PROMPT);
      } catch (IOException e) {
        System.err.println("An error was detected when we tried to read a line.");
        e.printStackTrace();
        System.exit(1);
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
          cmd.act(arguments.trim(), history, server, r);

          // reset the prompt (history might have changed)
          promptPrefix = genPromptPrefix(history, promptPrefix);
        } else {
          System.err.println("No command '" + splitString[0] + "' was found");
        }
      } else {
        try {
          // Play the stuff we just got, with history as context
          server.play(input, history.toString(), null, null, false);

          // If we have no exceptions, add to history
          history.append(input);
          history.append("\n");

          // If we're good, we should check to see if we reset the instrument
          promptPrefix = genPromptPrefix(input, promptPrefix);
        } catch (Throwable e) {
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
