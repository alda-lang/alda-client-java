package alda.repl.commands;

import alda.AldaResponse.AldaScore;
import alda.AldaServer;
import alda.error.NoResponseException;

import java.util.function.Consumer;

import jline.console.ConsoleReader;

public class ReplDown implements ReplCommand {
  @Override
  public void act(String args, StringBuffer history, AldaServer server,
                  ConsoleReader reader, Consumer<AldaScore> newInstrument)
  throws NoResponseException {
    server.setQuiet(false);
    server.down();
    server.setQuiet(true);
  }

  @Override
  public String docSummary() {
    return "Stops the Alda server.";
  }

  @Override
  public String key() {
    return "down";
  }
}
