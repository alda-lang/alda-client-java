package alda.repl.commands;

import alda.AldaServer;
import alda.AldaResponse.AldaScore;
import alda.error.NoAvailableWorkerException;
import alda.error.NoResponseException;
import alda.error.UnsuccessfulException;
import java.util.function.Consumer;
import jline.console.ConsoleReader;

/**
 * Handles the :export command
 */
public class ReplExport implements ReplCommand {

  public void act(
    String args, StringBuffer history, AldaServer server, ConsoleReader reader,
    Consumer<AldaScore> newInstrument
  ) throws NoResponseException {
    // TODO: In the future, if/when we add more output formats, add an "as
    // FORMAT" option, e.g. ":export something.mxml as musicxml".
    //
    // As of now, the only supported output format is MIDI, so there is no need
    // to make output format an option just yet. If/when we add other output
    // formats, we can attempt to determine the output format based on the
    // extension when the "as FORMAT" option is omitted.
    String filename = args;

    try {
      server.export(history.toString(), "midi", filename);
    } catch (NoResponseException e) {
      // Let the REPL handle the exception by offering to start the server.
      throw e;
    } catch (NoAvailableWorkerException | UnsuccessfulException e) {
      server.error(e.getMessage());
    }
  }

  @Override
  public String docSummary() {
    return "Exports the current score as a MIDI file.";
  }

  @Override
  public String docDetails() {
    return "Example usage:\n\n" +
      "  :export my-score.mid";
  }

  @Override
  public String key() {
    return "export";
  }
}
