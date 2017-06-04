package alda.repl.commands;

import alda.AldaResponse.AldaScore;
import alda.AldaServer;

import java.util.function.Consumer;

import jline.console.ConsoleReader;

public class ReplDown implements ReplCommand {
  public ReplDown() {}

  @Override
  public void act(String args, StringBuffer history, AldaServer server,
                  ConsoleReader reader, Consumer<AldaScore> newInstrument)
  throws alda.NoResponseException {
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
