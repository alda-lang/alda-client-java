package alda.repl.commands;

import alda.AldaResponse.AldaScore;
import alda.AldaServer;
import alda.error.NoResponseException;
import alda.error.UnsuccessfulException;

import java.util.function.Consumer;

import jline.console.ConsoleReader;

public class ReplStop implements ReplCommand {
  @Override
  public void act(String args, StringBuffer history, AldaServer server,
                  ConsoleReader reader, Consumer<AldaScore> newInstrument)
  throws NoResponseException {
    try {
      server.stop();
    } catch (UnsuccessfulException e) {
      server.error(e.getMessage());
    }
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
