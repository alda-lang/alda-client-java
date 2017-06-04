
package alda.repl.commands;

import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.function.Consumer;

import alda.AldaServer;
import alda.AldaResponse.AldaScore;
import jline.console.ConsoleReader;

/**
 * A interface that represents a ReplCommand
 *
 * A repl command is a explicit command that the client must process itself
 * such as :help, :load, or :play
 */
public interface ReplCommand {

  public static final Set<String> YES_ALIASES = new HashSet<>(Arrays.asList("yes", "true", "on", "+", "y"));
  public static final Set<String> NO_ALIASES = new HashSet<>(Arrays.asList("no", "false", "off", "-", "n"));

  /**
   * Runs this ReplCommand given the selected history.
   * This ReplCommand will modify the StringBuffer passed in to do it's actions.
   * @param args The arguments to this repl command
   * @param history The current history string for this repl
   * @param server The server to play pull data from, if needed.
   * @param reader A consoleReader so this command can prompt for user input
   * @param newInstrument A function to call if the current instrument changes.
   *   The new current instrument will be passed in as a string.
   */
  public void act(String args, StringBuffer history, AldaServer server,
                  ConsoleReader reader, Consumer<AldaScore> newInstrument)
  throws alda.NoResponseException;

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
   * Prints usage information for this command.
   * Useful when this command encounters an error
   */
  public default void usage() {
    System.err.println(docDetails());
  }

  /**
   * Gets the key that triggers this command
   * @return the key to trigger, eg: 'help'
   */
  public String key();
}
