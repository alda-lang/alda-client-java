package alda.repl.commands;

import alda.AldaResponse.AldaScore;
import alda.AldaServer;

import java.util.function.Consumer;

import jline.console.ConsoleReader;

public class ReplStatus implements ReplCommand {
  @Override
  public void act(String args, StringBuffer history, AldaServer server,
                  ConsoleReader reader, Consumer<AldaScore> newInstrument)
  throws alda.NoResponseException {
    server.setQuiet(false);
    server.status();
    server.setQuiet(true);
  }

  @Override
  public String docSummary() {
    return "Displays the status of the Alda server.";
  }

  @Override
  public String docDetails() {
    return "Status information includes:\n" +
           "  * whether the server is up or down\n" +
           "  * the backend port on which the server and workers communicate\n" +
           "  * many worker processes are currently available";
  }

  @Override
  public String key() {
    return "status";
  }
}
