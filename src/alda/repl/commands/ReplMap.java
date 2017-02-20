package alda.repl.commands;

import alda.AldaServer;
import alda.Util;

import jline.console.ConsoleReader;

/**
 * Handles the alda :map command
 * Prints out the current 'history' buffer
 */
public class ReplMap implements ReplCommand {
  @Override
  public void act(String args, StringBuffer history, AldaServer server, ConsoleReader reader) {
    try {
      String mode = Util.scoreMode(false, true);
      String res = server.parseRaw(history.toString(), mode, false);
      // TODO format this string to be pretty!
      System.out.println(res);
    } catch (Throwable e) {
      server.error(e.getMessage());
    }
  }
  @Override
  public String docSummary() {
    return "Prints the data representation of the score in progress.";
  }
  @Override
  public String key() {
    return "map";
  }
}
