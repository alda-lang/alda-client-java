package alda.repl.commands;

import alda.AldaServer;
import alda.AldaRequest;
import alda.AldaResponse.AldaScore;
import java.util.function.Consumer;
import jline.console.ConsoleReader;

import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

/**
 * Handles the :debug alda command
 * Toggles debug status for the alda client
 */
public class ReplDebug implements ReplCommand {

  // TODO find a better place to share these lists from
  public static final Set<String> YES_ALIASES = new HashSet<>(Arrays.asList("yes", "true", "on", "+"));
  public static final Set<String> NO_ALIASES = new HashSet<>(Arrays.asList("no", "false", "off", "-"));

  @Override
  public void act(String args, StringBuffer history, AldaServer server,
                  ConsoleReader reader, Consumer<AldaScore> newInstrument) {
    // If no args, toggle debug flag
    if (args.length() == 0) {
      AldaRequest.setDebug(!AldaRequest.getDebug());
    } else if (YES_ALIASES.contains(args.toLowerCase())) {
      // force on
      AldaRequest.setDebug(true);
    } else if (NO_ALIASES.contains(args.toLowerCase())) {
      // force off
      AldaRequest.setDebug(false);
    } else {
      System.err.println("'" + args + "' was not understood.");
      return;
    }
    System.out.println("Debug: " + AldaRequest.getDebug());
  }
  @Override
  public String docSummary() {
    return "Toggles debug on alda responses from the server.";
  }
  @Override
  public String key() {
    return "debug";
  }
}
