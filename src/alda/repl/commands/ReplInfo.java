package alda.repl.commands;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import alda.AldaResponse.AldaScore;
import alda.AldaServer;
import alda.Util;
import alda.error.NoResponseException;
import alda.error.ParseError;
import jline.console.ConsoleReader;

public class ReplInfo implements ReplCommand {

  private Gson gson = new Gson();
  private final String NO_RESULTS_PLACEHOLDER = "(none)";

  @Override
  public void act(String args, StringBuffer history, AldaServer server,
                  ConsoleReader reader, Consumer<AldaScore> newInstrument)
                      throws NoResponseException {
    try {
      String res = server.parseRaw(history.toString(), "data");

      if(res == null) {
        System.err.println("An internal error occurred when reading the score.");
      } else {
          JsonObject scoreMap = gson.fromJson(res, JsonObject.class);

          System.out.println(getScoreInfoText(scoreMap));
      }

    } catch(ParseError | JsonParseException e) {
      server.error(e.getMessage());
    }
  }

  private StringBuilder getScoreInfoText(JsonObject scoreMap) throws ParseError, JsonParseException {
    StringBuilder sb = new StringBuilder();
    sb.append("Instruments: ")
      .append(getInstrumentsString(scoreMap))
      .append(System.lineSeparator());

    sb.append("Current instruments: ")
      .append(getCurrentInstrumentsString(scoreMap))
      .append(System.lineSeparator());

    sb.append("Events: ")
      .append(getEventsAmount(scoreMap))
      .append(System.lineSeparator());

    sb.append("Markers: ")
      .append(getMarkersString(scoreMap))
      .append(System.lineSeparator());

    return sb;
  }

  private String getInstrumentsString(JsonObject scoreMap) {
    JsonObject instrJson = scoreMap.getAsJsonObject("instruments");
    String instr = instrJson.entrySet()
      .stream()
      .map(e -> e.getKey())
      .collect(Collectors.joining(", "));
    return instr.length() == 0 ? NO_RESULTS_PLACEHOLDER : instr;
  }

  private String getCurrentInstrumentsString(JsonObject scoreMap) {
    JsonArray curInstrJson = scoreMap.getAsJsonArray("current-instruments");
    List<String> curInstrLst = gson.fromJson(curInstrJson, new TypeToken<List<String>>(){}.getType());
    String instr = curInstrLst
      .stream()
      .collect(Collectors.joining(", "));
    return instr.length() == 0 ? NO_RESULTS_PLACEHOLDER : instr;
  }

  private String getMarkersString(JsonObject scoreMap) {
    JsonObject markersJson = scoreMap.getAsJsonObject("markers");
    String markers = markersJson.entrySet()
      .stream()
      .sorted(Comparator.comparing(Map.Entry::getValue, Util.JsonElementFloatComparator.INSTANCE))
      .map(e -> e.getKey())
      .collect(Collectors.joining(", "));
    return markers.length() == 0 ? NO_RESULTS_PLACEHOLDER : markers;
  }

  private int getEventsAmount(JsonObject scoreMap) {
    JsonArray eventsJson = scoreMap.getAsJsonArray("events");
    int nEvents = eventsJson.size();
    return nEvents;
  }

  @Override
  public String docSummary() {
    return "Prints information about the current score.";
  }

  @Override
  public String key() {
    return "info";
  }
}
