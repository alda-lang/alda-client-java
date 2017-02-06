
package alda.repl.commands;

import alda.AldaServer;

/**
 * A interface that represents a ReplCommand
 *
 * A repl command is a explicit command that the client must process itself
 * such as :help, :load, or :play
 */
public interface ReplCommand {


  /**
   * Runs this ReplCommand given the selected history.
   * This ReplCommand will modify the StringBuffer passed in to do it's actions.
   * @param args The arguments to this repl command
   * @param history The current history string for this repl
   * @param server The server to play pull data from, if needed.
   */
  public void act(String args, StringBuffer history, AldaServer server);

  /**
   * Returns the documentation summary of this ReplCommand
   * @return The summary that correponds to this ReplCommand
   */
  public default String docSummary() {
    return "Help is not available for this command.";
  }

  /**
   * Returns the documentation details of this ReplCommand
   * @return The details that correponds to this ReplCommand
   * If no details are available, return empty string. (or just don't override)
   */
  public default String docDetails() {
    return "";
  }

  /**
   * Gets the key that triggers this command
   * @return the key to trigger, eg: 'help'
   */
  public String key();
}
