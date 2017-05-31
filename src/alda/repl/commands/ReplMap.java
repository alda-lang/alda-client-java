package alda.repl.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import alda.AldaServer;
import alda.AldaResponse.AldaScore;
import alda.Util;

import java.util.function.Consumer;
import jline.console.ConsoleReader;

/**
 * Handles the alda :map command
 * Prints out the current 'history' buffer
 */
public class ReplMap implements ReplCommand {
  @Override
  public void act(String args, StringBuffer history, AldaServer server,
                  ConsoleReader reader, Consumer<AldaScore> newInstrument) {
    try {
      String res = server.parseRaw(history.toString(), false);

      if (res != null) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject json = gson.fromJson(res, JsonObject.class);

        System.out.println(gson.toJson(json));
      } else {
        System.err.println("An internal error occured when reading the map.");
      }
    } catch (Throwable e) {
      server.error(e.getMessage());
    }
  }
  @Override
  public String docSummary() {
    return "Prints the data representation of the score in progress.";
  }
  @Override
  public String key() {
    return "map";
  }
}
