package alda.repl.commands;

import alda.AldaServer;
import alda.AldaResponse.AldaScore;
import java.util.function.Consumer;
import jline.console.ConsoleReader;

/**
 * Handles the :play command
 * Takes from and to arguments, see documentation
 */
public class ReplPlay implements ReplCommand {

  public void act(String args, StringBuffer history, AldaServer server,
                  ConsoleReader reader, Consumer<AldaScore> newInstrument) {
    // Parse from/to args
    String[] arguments = args.split("\\s+");
    String from = "";
    String to = "";

    if (arguments.length > 0 && !args.equals("")) {
      for (int i = 0; i < arguments.length; i++) {
        if (arguments[i].equals("from")) {
          // process from argument
          if (++i >= arguments.length) {
            usage();
            return;
          }
          from = arguments[i];
        } else if (arguments[i].equals("to")) {
          // process to argument
          if (++i >= arguments.length) {
            usage();
            return;
          }
          to = arguments[i];
        } else {
          usage();
          return;
        }
      }
    }

    try {
      server.play(history.toString(), "",
                  from.length() > 0 ? from : null,
                  to.length() > 0 ? to : null, false);
    } catch (Throwable e) {
      server.error(e.getMessage());
    }
  }
  @Override
  public String docSummary() {
    return "Plays the current score.";
  }
  @Override
  public String docDetails() {
    return "Can take optional `from` and `to` arguments, in the form of markers or mm:ss times.\n\n" +
      "Without arguments, will play the entire score from beginning to end.\n\n" +
      "Example usage:\n\n" +
      "  :play\n" +
      "  :play from 0:05\n" +
      "  :play to 0:10\n" +
      "  :play from 0:05 to 0:10\n" +
      "  :play from guitarIn\n" +
      "  :play to verse\n" +
      "  :play from verse to bridge";
  }
  @Override
  public String key() {
    return "play";
  }
}
