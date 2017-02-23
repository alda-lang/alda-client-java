package alda.repl.commands;

import alda.AldaServer;
import jline.console.ConsoleReader;

/**
 * Handles the alda :score command
 * Prints out the current 'history' buffer
 */
public class ReplScore implements ReplCommand {
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
