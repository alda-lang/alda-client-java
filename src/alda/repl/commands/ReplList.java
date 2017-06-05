package alda.repl.commands;

import alda.AldaClient;
import alda.AldaResponse.AldaScore;
import alda.AldaServer;

import java.io.IOException;
import java.util.function.Consumer;

import jline.console.ConsoleReader;

public class ReplList implements ReplCommand {
  private static int LIST_PROCESSES_TIMEOUT = 5000;

  @Override
  public void act(String args, StringBuffer history, AldaServer server,
                  ConsoleReader reader, Consumer<AldaScore> newInstrument)
  throws alda.NoResponseException {
    try {
      AldaClient.listProcesses(LIST_PROCESSES_TIMEOUT);
    } catch (IOException e) {
      System.out.println("Error trying to list Alda processes:");
      e.printStackTrace();
    }
  }

  @Override
  public String docSummary() {
    return "Lists running Alda processes.";
  }

  @Override
  public String key() {
    return "list";
  }
}
