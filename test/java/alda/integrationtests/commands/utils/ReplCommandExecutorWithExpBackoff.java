package alda.integrationtests.commands.utils;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Optional;
import java.util.function.Consumer;

import alda.AldaResponse.AldaScore;
import alda.AldaServer;
import jline.console.ConsoleReader;
import alda.repl.commands.ReplCommand;

public class ReplCommandExecutorWithExpBackoff {

  private static final int EXP_BACKOFF_MAX_RETRIES = 5;
  private static final int EXP_BACKOFF_INITIAL_DELAY_MS = 2500;
  
  private final ReplCommand cmd;
  
  public ReplCommandExecutorWithExpBackoff(ReplCommand cmd) {
    this.cmd = cmd;
  }
  
  public Optional<String> executeReplCommandWithExpBackoff(String args, StringBuffer history, AldaServer server,
      ConsoleReader reader, Consumer<AldaScore> newInstrument) {
    String result = null;
    
    // Redirect StdOut
    ByteArrayOutputStream stdOutContent = new ByteArrayOutputStream();
    PrintStream oldStdOut = System.out;
    System.setOut(new PrintStream(stdOutContent));
    
    try {
      int expBoffNumRetry = 0;
      int expBoffDelayMs = EXP_BACKOFF_INITIAL_DELAY_MS;
      boolean waitingForWorkers;
      do {
        waitingForWorkers = false;
        if(expBoffNumRetry > 0) {
          System.err.println("Retrying in "+ expBoffDelayMs +"ms...");
          Thread.sleep(expBoffDelayMs);
          expBoffDelayMs = (int) Math.pow(expBoffDelayMs, 2);
        }
        cmd.act(args, history, server, reader, newInstrument);
        result = stdOutContent.toString();
        
        if(result != null
          && result.contains("No worker processes are ready yet. Please wait a minute.")) {
          waitingForWorkers = true;
          stdOutContent.reset();
          result = null;
        }
      } while(waitingForWorkers
          && expBoffNumRetry++ < EXP_BACKOFF_MAX_RETRIES);
    } catch(Exception e) {
      System.err.println("Error while executing repl command. "+ e.getMessage());
    }
    // Reset StdOut
    System.setOut(oldStdOut);
    stdOutContent.reset();
    
    return Optional.ofNullable(result);
  }
  
}
