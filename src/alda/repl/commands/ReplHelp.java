
package alda.repl.commands;

import alda.AldaServer;
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
