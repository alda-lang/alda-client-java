package alda.repl.commands;

import alda.AldaClient;
import alda.AldaResponse.AldaScore;
import alda.AldaServer;

import java.util.function.Consumer;

import jline.console.ConsoleReader;

public class ReplVersion implements ReplCommand {
  public ReplVersion() {}

  @Override
  public void act(String args, StringBuffer history, AldaServer server,
                  ConsoleReader reader, Consumer<AldaScore> newInstrument)
  throws alda.NoResponseException {
    System.out.println("Client version: " + AldaClient.version());
    System.out.print("Server version: ");
    server.setQuiet(false);
    server.version();
    server.setQuiet(true);
  }

  @Override
  public String docSummary() {
    return "Displays the version numbers of the Alda server and client.";
  }

  @Override
  public String key() {
    return "version";
  }
}
