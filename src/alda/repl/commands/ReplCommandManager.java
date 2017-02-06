

package alda.repl.commands;

import java.util.Map;
import java.util.HashMap;

import alda.AldaServer;

public class ReplCommandManager {

  private Map<String, ReplCommand> commands;

  public ReplCommandManager() {
    commands = new HashMap<>();

    // Temp array to store commands so we can iterate over them later
    ReplCommand[] cmds = {new ReplHelp(this),
                          new ReplScore()};

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

  // List of commands, represented as classes
  private class ReplScore implements ReplCommand {
    public void act(String args, StringBuffer history, AldaServer server) {
      return;
    }

    public String docstring() {
      return "Prints the score";
    }

    public String key() {
      return "score";
    }
  }
  
  private class ReplHelp implements ReplCommand {

    private ReplCommandManager cmdManager;

    public static final String NO_DOCSTRING
      = "No documentation was found for this command.";

    public ReplHelp(ReplCommandManager m) {
      cmdManager = m;
    }

    public void act(String args, StringBuffer history, AldaServer server) {
      args = args.trim();
      ReplCommand cmd = cmdManager.get(args);
      if (cmd != null) {
        System.out.println(cmd.docstring());
      } else {
        System.err.println(NO_DOCSTRING);
      }
      return;
    }

    public String docstring() {
      return "The command that gives you help";
    }

    public String key() {
      return "help";
    }
  }

}
