
package alda.repl.commands;

import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Iterator;

import jline.console.ConsoleReader;
import alda.AldaServer;

/**
 * Class to manage and store all ReplCommands.
 * The ReplHelp objecct uses this to generate it's list of documentation.
 */
public class ReplCommandManager implements Iterable<ReplCommand> {

  private Map<String, ReplCommand> commands;

  public ReplCommandManager() {
    commands = new HashMap<>();

    // To see the actual command implemnetations, see Repl*.java (for each command)

    // Temp array to store commands so we can iterate over them later
    ReplCommand[] cmds = {new ReplPlay(),
                          new ReplNew(this),
                          new ReplQuit(),
                          new ReplScore(),
                          new ReplMap(),
                          new ReplLoad(),
                          new ReplSave(),
                          new ReplDebug(),
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

  /**
   * Runs a function over every command we have stored.
   * @param func The function to run on each command.
   */
  public Iterator<ReplCommand> iterator() {
    // Create a iterator over all the values in command.
    return commands.entrySet()
      .stream()
      .map(Map.Entry::getValue)
      .iterator();
  }
}
