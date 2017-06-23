package alda.repl.commands;

import alda.AldaResponse.AldaScore;
import alda.AldaServer;
import alda.error.AlreadyUpException;
import alda.error.InvalidOptionsException;
import alda.error.NoResponseException;
import alda.repl.AldaRepl;

import java.util.function.Consumer;

import jline.console.ConsoleReader;

public class ReplUp implements ReplCommand {
  @Override
  public void act(String args, StringBuffer history, AldaServer server,
                  ConsoleReader reader, Consumer<AldaScore> newInstrument)
  throws NoResponseException {
    server.setQuiet(false);
    try {
      server.upBg(AldaRepl.DEFAULT_NUMBER_OF_WORKERS);
    } catch (InvalidOptionsException | AlreadyUpException |
             alda.error.IOException e) {
      System.err.println("Unable to start server: ");
      e.printStackTrace();
    }
    server.setQuiet(true);
  }

  @Override
  public String docSummary() {
    return "Starts the Alda server.";
  }

  @Override
  public String key() {
    return "up";
  }
}
