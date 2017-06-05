package alda.repl.commands;

import alda.AldaResponse.AldaScore;
import alda.AldaServer;
import alda.repl.AldaRepl;

import java.util.function.Consumer;

import jline.console.ConsoleReader;

public class ReplDownUp implements ReplCommand {
  @Override
  public void act(String args, StringBuffer history, AldaServer server,
                  ConsoleReader reader, Consumer<AldaScore> newInstrument)
  throws alda.NoResponseException {
    server.setQuiet(false);
    try {
      server.downUp(AldaRepl.DEFAULT_NUMBER_OF_WORKERS);
    } catch (alda.InvalidOptionsException e) {
      System.err.println("Unable to start server: ");
      e.printStackTrace();
    }
    server.setQuiet(true);
  }

  @Override
  public String docSummary() {
    return "Restarts the Alda server.";
  }

  @Override
  public String key() {
    return "downup";
  }
}
