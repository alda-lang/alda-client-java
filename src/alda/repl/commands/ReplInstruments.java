package alda.repl.commands;

import alda.AldaResponse.AldaScore;
import alda.AldaServer;
import alda.error.NoResponseException;

import java.util.function.Consumer;

import jline.console.ConsoleReader;

public class ReplInstruments implements ReplCommand {
  @Override
  public void act(
    String args, StringBuffer history, AldaServer server, ConsoleReader reader,
    Consumer<AldaScore> newInstrument
  ) throws NoResponseException {
    server.displayInstruments();
  }

  @Override
  public String docSummary() {
    return "Display a list of available instruments";
  }

  @Override
  public String key() {
    return "instruments";
  }
}
