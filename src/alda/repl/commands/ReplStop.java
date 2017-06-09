package alda.repl.commands;

import alda.AldaResponse.AldaScore;
import alda.AldaServer;

import java.util.function.Consumer;

import jline.console.ConsoleReader;

public class ReplStop implements ReplCommand {
  @Override
  public void act(String args, StringBuffer history, AldaServer server,
                  ConsoleReader reader, Consumer<AldaScore> newInstrument)
  throws alda.NoResponseException {
    server.stop();
  }

  @Override
  public String docSummary() {
    return "Stops playback.";
  }

  @Override
  public String key() {
    return "stop";
  }
}
