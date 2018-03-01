package alda.repl.commands;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.google.gson.JsonParseException;

import alda.AldaResponse.AldaScore;
import alda.AldaServer;
import alda.error.NoResponseException;
import alda.error.ParseError;
import jline.console.ConsoleReader;

public class ReplInfo implements ReplCommand {
  
  private Gson gson = new Gson();
	
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
    		.append(getMarkersAmount(scoreMap))
    		.append(System.lineSeparator());
        
		System.out.println(sb);
	  }
	  
	} catch(ParseError e) {
	  server.error(e.getMessage());
	} catch(JsonParseException e) {
	  server.error(e.getMessage());
	} catch(ClassCastException e) {
	  server.error(e.getMessage());
	}
    
  }
  
  private String getInstrumentsString(JsonObject scoreMap) {
    JsonObject instrJson = scoreMap.getAsJsonObject("instruments");
	String instr = instrJson.entrySet()
		.stream()
		.map(e -> e.getKey())
		.collect(Collectors.joining(", "));
	return instr;
  }

  private String getCurrentInstrumentsString(JsonObject scoreMap) {
    JsonArray curInstrJson = scoreMap.getAsJsonArray("current-instruments");
    List<String> curInstrLst = gson.fromJson(curInstrJson, new TypeToken<List<String>>(){}.getType());
	String instr = curInstrLst
		.stream()
		.collect(Collectors.joining(", "));
	return instr;
  }
  
  private int getMarkersAmount(JsonObject scoreMap) {
    JsonObject markersJson = scoreMap.getAsJsonObject("markers");
    int nMarkers = markersJson.entrySet().size() - 1;
    return nMarkers;
  }
  
  private int getEventsAmount(JsonObject scoreMap) {
    JsonArray eventsJson = scoreMap.getAsJsonArray("events");
    int nEvents = eventsJson.size();
    return nEvents;
  }
  
  @Override
  public String docSummary() {
    return "Print current score info.";
  }

  @Override
  public String key() {
    return "info";
  }
}
