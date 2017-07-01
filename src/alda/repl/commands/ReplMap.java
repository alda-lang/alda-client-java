package alda.repl.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import alda.AldaServer;
import alda.AldaResponse.AldaScore;
import alda.error.NoResponseException;
import alda.error.ParseError;
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
                  ConsoleReader reader, Consumer<AldaScore> newInstrument)
  throws NoResponseException {
    try {
      String res = server.parseRaw(history.toString());

      if (res != null) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject json = gson.fromJson(res, JsonObject.class);

        System.out.println(gson.toJson(json));
      } else {
        System.err.println("An internal error occured when reading the map.");
      }
    } catch (NoResponseException e) {
      // Let the REPL handle the exception by offering to start the server.
      throw e;
    } catch (ParseError e) {
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
