package alda.repl.commands;

import alda.AldaServer;
import alda.AldaRequest;
import alda.AldaResponse.AldaScore;
import java.util.function.Consumer;
import jline.console.ConsoleReader;

/**
 * Handles the :debug alda command
 * Toggles debug status for the alda client
 */
public class ReplDebug implements ReplCommand {
  private boolean debug = false;
  @Override
  public void act(String args, StringBuffer history, AldaServer server,
                  ConsoleReader reader, Consumer<AldaScore> newInstrument) {
    debug = !debug;
    System.out.println("Debug: " + debug);
    AldaRequest.setDebug(debug);
  }
  @Override
  public String docSummary() {
    return "Toggles debug on alda responses from the server.";
  }
  @Override
  public String key() {
    return "debug";
  }
}
