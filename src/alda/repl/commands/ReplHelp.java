
package alda.repl.commands;

import alda.AldaServer;
import alda.AldaResponse.AldaScore;

import java.util.List;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import jline.console.ConsoleReader;

/**
 * Handles the :help command
 * Unlike the other commands, we need to have a ReplCommandManager object to
 * find out what commands we have available.
 */
public class ReplHelp implements ReplCommand {

  private ReplCommandManager cmdManager;

  public static final String HELP_HEADER
    = "For commands marked with (*), more detailed information about the command is available via the :help command.\n\n" +
    "e.g. :help play\n\n" +
    "Available commands:\n";

  public ReplHelp(ReplCommandManager m) {
    cmdManager = m;
  }

  @Override
  public void act(String args, StringBuffer history, AldaServer server,
                  ConsoleReader reader, Consumer<AldaScore> newInstrument) {
    args = args.trim();

    // the length of the longest command
    int maxKeyLength = cmdManager.values()
                                 .stream()
                                 .mapToInt(cmd -> cmd.key().length())
                                 .max()
                                 .getAsInt();

    // Print out default help info
    if (args.length() == 0) {
      System.out.println(HELP_HEADER);

      cmdManager
        .values()
        .stream()
        .sorted((cmd1, cmd2) -> cmd1.key().compareTo(cmd2.key()))
        .forEach(cmd -> {
          String key = cmd.key();
          int numberOfSpaces = maxKeyLength - key.length() + 2;
          List<String> spaces = Collections.nCopies(numberOfSpaces, " ");
          String spacing = String.join("", spaces);
          System.out.println("    :" + key + spacing +
                             cmd.docSummary() +
                             (cmd.hasDocDetails() ? " (*)" : ""));
        });

      System.out.println();

      return;
    }

    // Print out specialized help info like ':help help'
    ReplCommand cmd = cmdManager.get(args);
    if (cmd != null) {
      System.out.println(cmd.docSummary());
      if (cmd.docDetails() != "") {
        System.out.println();
        System.out.println(cmd.docDetails());
        System.out.println();
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
