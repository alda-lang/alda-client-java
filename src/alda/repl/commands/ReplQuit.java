package alda.repl.commands;

import alda.AldaServer;
import alda.Util;
import alda.AldaResponse.AldaScore;
import java.util.function.Consumer;
import jline.console.ConsoleReader;

/**
 * Handles the :quit alda command
 * Simply exits the repl
 */
public  class ReplQuit implements ReplCommand {
  @Override
  public void act(String args, StringBuffer history, AldaServer server,
                  ConsoleReader reader, Consumer<AldaScore> newInstrument) {
    if (history.length() == 0 ||
        Util.promptWithChoices(
          reader,
          "Are you sure you want to quit (destroying unsaved changes)?",
          "yes", "no").equals("yes")) {
      // Bye! =)
      System.exit(0);
    }
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
