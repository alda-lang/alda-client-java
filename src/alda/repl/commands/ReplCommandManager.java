
package alda.repl.commands;

import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.stream.Stream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.IOException;
import java.io.UncheckedIOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.FileAlreadyExistsException;

import jline.console.ConsoleReader;

import alda.AldaServer;

/**
 * Class to manage and store all ReplCommands.
 * The ReplHelp objecct uses this to generate it's list of documentation.
 */
public class ReplCommandManager {

  private Map<String, ReplCommand> commands;

  public ReplCommandManager() {
    commands = new HashMap<>();

    // Temp array to store commands so we can iterate over them later
    ReplCommand[] cmds = {new ReplPlay(),
                          new ReplNew(),
                          new ReplQuit(),
                          new ReplScore(),
                          new ReplLoad(),
                          new ReplSave(),
                          new ReplHelp(this)};

    for (ReplCommand c : cmds) {
      commands.put(c.key(), c);
    }
  }

  /**
   * Returns the ReplCommand that corresponds to this key
   * @param key the key to key into
   * @return the repl command key corresponds to.
   * null if no command corresponds to key
   */
  public ReplCommand get(String key) {
    return commands.getOrDefault(key, null);
  }

  /**
   * Gets a collection of all ReplCommands Available
   * @return A collection of all the commands we know about
   */
  public Collection<ReplCommand> values() {
    return commands.values();
  }

  //*************************List of Commands***********************************
  private class ReplPlay implements ReplCommand {
    @Override
    public void act(String args, StringBuffer history, AldaServer server, ConsoleReader reader) {
      // Parse from/to args
      String[] arguments = args.split("\\s+");
      String from = "";
      String to = "";

      if (arguments.length > 0 && !args.equals("")) {
        for (int i = 0; i < arguments.length; i++) {
          if (arguments[i].equals("from")) {
            // process from argument
            if (++i >= arguments.length) {
              usage();
              return;
            }
            from = arguments[i];
          } else if (arguments[i].equals("to")) {
            // process to argument
            if (++i >= arguments.length) {
              usage();
              return;
            }
            to = arguments[i];
          } else {
            usage();
            return;
          }
        }
      }

      try {
        server.play(history.toString(), "",
                    from.length() > 0 ? from : null,
                    to.length() > 0 ? to : null, false);
      } catch (Throwable e) {
        server.error(e.getMessage());
      }
    }
    @Override
    public String docSummary() {
      return "Plays the current score.";
    }
    @Override
    public String docDetails() {
      return "Can take optional `from` and `to` arguments, in the form of markers or mm:ss times.\n\n" +
        "Without arguments, will play the entire score from beginning to end.\n\n" +
        "Example usage:\n\n" +
        "  :play\n" +
        "  :play from 0:05\n" +
        "  :play to 0:10\n" +
        "  :play from 0:05 to 0:10\n" +
        "  :play from guitarIn\n" +
        "  :play to verse\n" +
        "  :play from verse to bridge";
    }
    @Override
    public String key() {
      return "play";
    }
  }

  private class ReplSave implements ReplCommand {
    private String oldSaveFile = null;
    @Override
    public void act(String args, StringBuffer history, AldaServer server, ConsoleReader reader) {
      // Turn ~ into home
      args = args.replaceFirst("^~",System.getProperty("user.home"));
      try {
        if (args.length() == 0) {
          if (oldSaveFile != null && oldSaveFile.length() != 0) {
            // Overwrite by default if running :save
            Files.write(Paths.get(oldSaveFile), history.toString().getBytes());
          } else {
            usage();
          }
          return;
        }

        try {
          Files.write(Paths.get(args), history.toString().getBytes(), StandardOpenOption.CREATE_NEW);
          oldSaveFile = args;
        } catch (FileAlreadyExistsException e) {
          String confirm = reader.readLine("File already present, overwrite? [y/n]: ");
          if (confirm.equalsIgnoreCase("y") || confirm.equalsIgnoreCase("yes")) {
            Files.write(Paths.get(args), history.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            oldSaveFile = args;
          }
        }
      } catch (IOException|UncheckedIOException e) {
        e.printStackTrace();
        System.err.println("There was an error writing to '" + args + "'");
      }
    }
    @Override
    public String docSummary() {
      return "Saves an the current REPL session into an Alda score.";
    }
    @Override
    public String docDetails() {
      return "Usage:\n\n" +
        "  :save test/examples/bach_cello_suite_no_1.alda\n" +
        "  :save ~/Scores/love_is_alright_tonite.alda\n\n" +
        "Once :save has been executed once:\n" +
        "  :save";
    }
    @Override
    public String key() {
      return "save";
    }
  }

  private class ReplLoad implements ReplCommand {
    @Override
    public void act(String args, StringBuffer history, AldaServer server, ConsoleReader reader) {

      if (args == "") {
        usage();
        return;
      }
      Stream<String> fLines = null;
      try {
        fLines = Files.lines(Paths.get(args));
        StringBuffer newHistory = new StringBuffer();
        fLines.forEach(x -> {
            newHistory.append(x);
            newHistory.append("\n");
          });
        history.delete(0, history.length());
        history.append(newHistory);
        // TODO check to see if score is valid by sending it to server (maybe with :to 0:00)
      } catch (IOException|UncheckedIOException e) {
        System.err.println("There was an error reading '" + args + "'");
      } finally {
        if (fLines != null)
          fLines.close();
      }
    }
    @Override
    public String docSummary() {
      return "Loads an Alda score into the current REPL session.";
    }
    @Override
    public String docDetails() {
      return "Usage:\n\n" +
        "  :load test/examples/bach_cello_suite_no_1.alda\n" +
        "  :load /Users/rick/Scores/love_is_alright_tonite.alda";
    }
    @Override
    public String key() {
      return "load";
    }
  }

  private class ReplNew implements ReplCommand {
    @Override
    public void act(String args, StringBuffer history, AldaServer server, ConsoleReader reader) {
      history.delete(0, history.length());
    }
    @Override
    public String docSummary() {
      return "Creates a new score.";
    }
    @Override
    public String key() {
      return "new";
    }
  }

  private class ReplQuit implements ReplCommand {
    @Override
    public void act(String args, StringBuffer history, AldaServer server, ConsoleReader reader) {
      // Bye! =)
      System.exit(0);
    }
    @Override
    public String docSummary() {
      return "Exits the Alda REPL session.";
    }
    @Override
    public String key() {
      return "quit";
    }
  }

  private class ReplScore implements ReplCommand {
    @Override
    public void act(String args, StringBuffer history, AldaServer server, ConsoleReader reader) {
      System.out.println(history.toString().trim());
    }
    @Override
    public String docSummary() {
      return "Prints the score (as Alda code).";
    }
    @Override
    public String key() {
      return "score";
    }
  }


  // Class that handles :help commands
  private class ReplHelp implements ReplCommand {

    private ReplCommandManager cmdManager;

    public static final String HELP_HEADER
      = "For commands marked with (*), more detailed information about the command is available via the :help command.\n\n" +
      "e.g. :help play\n\n" +
      "Available commands:\n";

    public ReplHelp(ReplCommandManager m) {
      cmdManager = m;
    }

    public void act(String args, StringBuffer history, AldaServer server, ConsoleReader reader) {
      args = args.trim();

      // Print out default help info
      if (args.length() == 0) {
        System.out.println(HELP_HEADER);
        for (ReplCommand c : cmdManager.values()) {
          System.out.println("    :" + c.key() + "\t" + c.docSummary()
                             + (c.docDetails() != "" ? " (*)" : ""));
        }
        return;
      }

      // Print out specialized help info like ':help help'
      ReplCommand cmd = cmdManager.get(args);
      if (cmd != null) {
        System.out.println(cmd.docSummary());
        if (cmd.docDetails() != "") {
          System.out.println();
          for (String line : cmd.docDetails().split("\n")) {
            System.out.println("   " + line);
          }
        }
      } else {
        System.err.println("No documentation was found for '" + args + "'.");
      }
      return;
    }

    @Override
    public String docSummary() {
      return "Display this help text.";
    }

    @Override
    public String docDetails() {
      return "Usage:\n\n" +
        "  :help help\n" +
        "  :help new";
    }

    @Override
    public String key() {
      return "help";
    }
  }
  //****************************************************************************
}
