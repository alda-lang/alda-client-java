package alda.repl.commands;

import alda.AldaClient;
import alda.AldaResponse.AldaScore;
import alda.AldaServer;
import alda.AldaServerOptions;
import alda.error.NoResponseException;
import alda.error.SystemException;

import java.util.function.Consumer;

import jline.console.ConsoleReader;

public class ReplList implements ReplCommand {
  private static int LIST_PROCESSES_TIMEOUT = 5000;

  @Override
  public void act(String args, StringBuffer history, AldaServer server,
                  ConsoleReader reader, Consumer<AldaScore> newInstrument)
  throws NoResponseException {
    try {
      AldaServerOptions serverOpts = new AldaServerOptions();
      serverOpts.noColor = server.noColor;
      serverOpts.timeout = LIST_PROCESSES_TIMEOUT;
      AldaClient.listProcesses(serverOpts);
    } catch (SystemException e) {
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
