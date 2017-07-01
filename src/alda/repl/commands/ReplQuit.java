package alda.repl.commands;

import alda.AldaResponse.AldaScore;
import alda.AldaServer;
import alda.error.ExitCode;
import alda.Util;
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
          "You have unsaved changes. Are you sure you want to quit?",
          "yes", "no").equals("yes")) {
      // Bye! =)
      ExitCode.SUCCESS.exit();
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
