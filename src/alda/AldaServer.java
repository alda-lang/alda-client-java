package alda;

import alda.error.AlreadyUpException;
import alda.error.InvalidOptionsException;
import alda.error.NoAvailableWorkerException;
import alda.error.NoResponseException;
import alda.error.ParseError;
import alda.error.SystemException;
import alda.error.UnsuccessfulException;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.UUID;

import org.apache.commons.lang3.SystemUtils;

import org.fusesource.jansi.AnsiConsole;
import static org.fusesource.jansi.Ansi.*;
import static org.fusesource.jansi.Ansi.Color.*;

public class AldaServer extends AldaProcess {
  private static final int PING_TIMEOUT = 100; // ms
  private static final int PING_RETRIES = 5;
  private static final int STARTUP_RETRY_INTERVAL = 250; // ms
  private static final int SHUTDOWN_RETRY_INTERVAL = 250; // ms
  private static final int STATUS_RETRY_INTERVAL = 200;  // ms
  private static final int STATUS_RETRIES = 10;
  private static final int PLAY_STATUS_INTERVAL = 250;   // ms

  // Relevant to playing input from the REPL.
  private static final int BUSY_WORKER_TIMEOUT = 10000;      // ms
  private static final int BUSY_WORKER_RETRY_INTERVAL = 500; // ms

  public AldaServer(AldaServerOptions opts) {
    host    = normalizeHost(opts.host);
    port    = opts.port;
    timeout = opts.timeout;
    verbose = opts.verbose;
    quiet   = opts.quiet;
    noColor = opts.noColor;

    if (!noColor) AnsiConsole.systemInstall();
  }

  // Calculate the number of retries before giving up, based on a fixed retry
  // interval and the desired overall timeout in seconds.
  private int calculateRetries(int timeout, int interval) {
    int retriesPerSecond = 1000 / interval;
    return timeout * retriesPerSecond;
  }

  private boolean ping(int timeout, int retries) throws NoResponseException {
    AldaRequest req = new AldaRequest(this.host, this.port);
    req.command = "ping";
    AldaResponse res = req.send(timeout, retries);
    return res.success;
  }

  public boolean checkForConnection(int timeout, int retries) {
    try {
      return ping(timeout, retries);
    } catch (NoResponseException e) {
      return false;
    }
  }

  public boolean checkForConnection() {
    return checkForConnection(PING_TIMEOUT, PING_RETRIES);
  }

  // Waits until the process is confirmed to be up, or we reach the timeout.
  //
  // Throws a NoResponseException if the timeout is reached.
  public void waitForConnection() throws NoResponseException {
    int retries = calculateRetries(timeout, STARTUP_RETRY_INTERVAL);

    if (!checkForConnection(STARTUP_RETRY_INTERVAL, retries)) {
      throw new NoResponseException(
        "Timed out waiting for response from the server."
      );
    }
  }

  // Waits until the process is confirmed to be down, i.e. there is no response
  // to "ping," OR, there is a response to "ping," but the response indicates
  // "success": false.
  //
  // Throws a NoResponseException if the process is still pingable after the
  // timeout.
  public void waitForLackOfConnection() throws NoResponseException {
    int retries = calculateRetries(timeout, SHUTDOWN_RETRY_INTERVAL);

    while (retries >= 0) {
      try {
        if (!ping(SHUTDOWN_RETRY_INTERVAL, 0)) return;
      } catch (NoResponseException e) {
        return;
      }

      Util.sleep(SHUTDOWN_RETRY_INTERVAL);
      retries--;
    }

    throw new NoResponseException(
      "Timed out waiting for the server to shut down."
    );
  }

  private static String normalizeHost(String host) {
    // trim leading/trailing whitespace and trailing "/"
    host = host.trim().replaceAll("/$", "");
    // prepend tcp:// if not already present
    if (!(host.startsWith("tcp://"))) {
      host = "tcp://" + host;
    }
    return host;
  }

  private void assertNotRemoteHost() throws InvalidOptionsException {
    String hostWithoutProtocol = host.replaceAll("tcp://", "");

    if (!hostWithoutProtocol.equals("localhost")) {
      throw new InvalidOptionsException(
          "Alda servers cannot be started remotely.");
    }
  }

  public void setQuiet(boolean quiet) {
    this.quiet = quiet;
  }

  public void msg(String message) {
    if (quiet)
      return;

    String hostWithoutProtocol = host.replaceAll("tcp://", "");

    String prefix;
    if (hostWithoutProtocol.equals("localhost")) {
      prefix = "";
    } else {
      prefix = hostWithoutProtocol + ":";
    }

    prefix += Integer.toString(port);

    if (noColor) {
      prefix = String.format("[%s] ", prefix);
    } else {
      prefix = String.format("[%s] ", ansi().fg(BLUE)
                                            .a(prefix)
                                            .reset()
                                            .toString());
    }

    System.out.println(prefix + message);
  }

  public void error(String message) {
    String prefix;
    if (noColor) {
      prefix = "ERROR ";
    } else {
      prefix = ansi().fg(RED).a("ERROR ").reset().toString();
    }

    // save and restore quiet value to print out errors
    boolean oldQuiet = quiet;
    quiet = false;
    msg(prefix + message);
    quiet = oldQuiet;
  }

  private final String CHECKMARK = "\u2713";
  private final String X = "\u2717";

  private void announceReady() {
    if (noColor) {
      msg("Ready " + CHECKMARK);
    } else {
      msg(ansi().a("Ready ").fg(GREEN).a(CHECKMARK).reset().toString());
    }
  }

  private void announceServerUp() {
    if (noColor) {
      msg("Server up " + CHECKMARK);
    } else {
      msg(ansi().a("Server up ").fg(GREEN).a(CHECKMARK).reset().toString());
    }
  }

  private void announceServerDown(boolean isGood) {
    Color color = isGood ? GREEN : RED;
    String glyph = isGood ? CHECKMARK : X;
    if (noColor) {
      msg("Server down " + glyph);
    } else {
      msg(ansi().a("Server down ").fg(color).a(glyph).reset().toString());
    }
  }

  private void announceServerDown() {
    announceServerDown(false);
  }

  public void upBg(int numberOfWorkers)
    throws InvalidOptionsException, NoResponseException, AlreadyUpException,
           SystemException {
    assertNotRemoteHost();

    boolean serverAlreadyUp = checkForConnection();
    if (serverAlreadyUp) {
      throw new AlreadyUpException("Server already up.");
    }

    boolean serverAlreadyTryingToStart;
    try {
      serverAlreadyTryingToStart = SystemUtils.IS_OS_UNIX &&
                                   AldaClient.checkForExistingServer(port);
    } catch (SystemException e) {
      System.out.println("WARNING: Unable to detect whether or not there is " +
                         "already a server running on that port.");
      serverAlreadyTryingToStart = false;
    }

    if (serverAlreadyTryingToStart) {
      throw new AlreadyUpException(
        "There is already a server trying to start on this port. Please be " +
        "patient -- this can take a while."
      );
    }

    Object[] opts = {"--host", host,
                     "--port", Integer.toString(port),
                     "--workers", Integer.toString(numberOfWorkers),
                     "--alda-fingerprint"};

    try {
      Util.forkProgram(Util.conj(opts, "server"));
      msg("Starting Alda server...");
      waitForConnection();
      announceServerUp();
    } catch (URISyntaxException e) {
      throw new SystemException(
        String.format("Unable to fork '%s' into the background."), e
      );
    } catch (IOException e) {
      throw new SystemException("Unable to fork a background process.", e);
    }

    msg("Starting worker processes...");

    int workersAvailable = 0;
    while (workersAvailable == 0) {
      Util.sleep(STARTUP_RETRY_INTERVAL);
      AldaRequest req = new AldaRequest(host, port);
      req.command = "status";
      AldaResponse res = req.send();
      if (res.body.contains("Server up")) {
        Matcher a = Pattern.compile("(\\d+)/\\d+ workers available")
                           .matcher(res.body);
        if (a.find()) {
          workersAvailable = Integer.parseInt(a.group(1));
        }
      }
    }

    announceReady();
  }

  public void upFg(int numberOfWorkers) throws InvalidOptionsException {
    assertNotRemoteHost();

    Object[] args = {numberOfWorkers, port, verbose};

    Util.callClojureFn("alda.server/start-server!", args);
  }

  public void down() throws NoResponseException {
    boolean serverAlreadyDown = !checkForConnection();
    if (serverAlreadyDown) {
      msg("Server already down.");
      return;
    }

    msg("Stopping Alda server...");

    AldaRequest req = new AldaRequest(host, port);
    req.command = "stop-server";

    try {
      AldaResponse res = req.send();
      if (res.success) {
        announceServerDown(true);
      } else {
        throw new NoResponseException("Failed to stop server.");
      }
    } catch (NoResponseException e) {
      announceServerDown(true);
    }
  }

  public void downUp(int numberOfWorkers)
    throws NoResponseException, AlreadyUpException, InvalidOptionsException,
           SystemException {
    down();

    waitForLackOfConnection();
    // The process can still hang around sometimes, causing upBg to fail if we
    // try to do it too soon. Giving it a little extra time here as a buffer.
    Util.sleep(1000);

    System.out.println();
    upBg(numberOfWorkers);
  }

  public void status() {
    AldaRequest req = new AldaRequest(host, port);
    req.command = "status";

    try {
      AldaResponse res = req.send(STATUS_RETRY_INTERVAL, STATUS_RETRIES);
      if (!res.success) throw new UnsuccessfulException(res.body);
      msg(res.body);
    } catch (NoResponseException e) {
      announceServerDown();
    } catch (UnsuccessfulException e) {
      msg("Unable to report status.");
    }
  }

  public void version() throws NoResponseException {
    AldaRequest req = new AldaRequest(host, port);
    req.command = "version";
    AldaResponse res = req.send();
    String serverVersion = res.body;

    msg(serverVersion);
  }

  public AldaResponse play(String code, String from, String to)
    throws NoAvailableWorkerException, UnsuccessfulException,
           NoResponseException {
    return play(code, null, from, to);
  }

  /**
   * Tries to play a bit of alda code
   *
   * @param code The pimary code to play
   * @param history The history context to supplement code
   * @param from Time to play from
   * @param to Time to stop playing
   * @return The response from the play, with useful information.
   */
  public AldaResponse play(String code, String history, String from, String to)
    throws NoAvailableWorkerException, UnsuccessfulException,
           NoResponseException {

    String jobId = UUID.randomUUID().toString();

    AldaRequest req = new AldaRequest(host, port);
    req.command = "play";
    req.body = code;
    req.options = new AldaRequestOptions();
    req.options.jobId = jobId;

    if (from != null) {
      req.options.from = from;
    }

    if (to != null) {
      req.options.to = to;
    }

    if (history != null) {
      req.options.history = history;
    }

    // play requests need to be sent exactly once and not retried, otherwise
    // the score could be played more than once.
    //
    // play requests are asynchronous; the response from the worker should be
    // immediate, and then in the code below, we repeatedly ask the worker for
    // status and send updates to the user until the status is "playing."
    AldaResponse res = req.send(3000, 0);

    if (!res.success) {
      String noWorkersYetMsg = "No worker processes are ready yet";
      String workersBusyMsg = "All worker processes are currently busy";

      if (res.body.contains(noWorkersYetMsg) ||
          res.body.contains(workersBusyMsg)) {
        throw new NoAvailableWorkerException(res.body);
      } else {
        throw new UnsuccessfulException(res.body);
      }
    }

    if (res.workerAddress == null) {
      throw new UnsuccessfulException(
        "No worker address included in response; unable to check for status."
      );
    }

    String status = "requested";

    while (true) {
      AldaResponse update = playStatus(res.workerAddress, jobId);

      // Ensures that any update we process is for this score, and not a
      // previous one.
      if (!update.jobId.equals(jobId)) continue;

      // Bail out if there was some problem server-side.
      if (!update.success) throw new UnsuccessfulException(update.body);

      // Update the job status if it's different.
      if (!update.body.equals(status)) {
        status = update.body;
        switch (status) {
          case "parsing": msg("Parsing/evaluating..."); break;
          case "playing": msg("Playing..."); break;
          // In rare cases (i.e. when the score is really short), the worker can
          // be done playing the score already.
          case "success": msg("Done playing."); break;
          default: msg(status);
        }
      }

      // If the job is still pending, pause and then keep looping.
      if (update.pending) {
        Util.sleep(PLAY_STATUS_INTERVAL);
      } else {
        // We succeeded!
        return update;
      }
    }
  }

  public AldaResponse playFromRepl(String input, String history, String from,
                                   String to)
    throws NoAvailableWorkerException, UnsuccessfulException,
           NoResponseException {
    int retries = BUSY_WORKER_TIMEOUT / BUSY_WORKER_RETRY_INTERVAL;
    return playFromRepl(input, history, from, to, retries);
  }

  public AldaResponse playFromRepl(String input, String history, String from,
                                   String to, int retries)
    throws NoAvailableWorkerException, UnsuccessfulException,
           NoResponseException {
    // Placeholder exception; we should never see this get thrown.
    String msg = "Unexpected error trying to play input from the Alda REPL.";
    NoAvailableWorkerException error = new NoAvailableWorkerException(msg);

    // Retry until we get a NoAvailableWorkerException `retries` times.
    while (retries >= 0) {
      try {
        return play(input, history.toString(), from, to);
      } catch (NoAvailableWorkerException e) {
        error = e;
        Util.sleep(BUSY_WORKER_RETRY_INTERVAL);
        retries--;
      }
    }

    // Throw the most recent NoAvailableWorkerException before we ran out of
    // retries.
    throw error;
  }


  public AldaResponse playStatus(byte[] workerAddress, String jobId)
    throws NoResponseException {
    AldaRequest req = new AldaRequest(host, port);
    req.command = "play-status";
    req.workerToUse = workerAddress;
    req.options = new AldaRequestOptions();
    req.options.jobId = jobId;
    return req.send();
  }

  public void stop() throws UnsuccessfulException {
    AldaRequest req = new AldaRequest(host, port);
    req.command = "stop-playback";

    try {
      AldaResponse res = req.send();
      if (!res.success) throw new UnsuccessfulException(res.body);
      msg(res.body);
    } catch (NoResponseException e) {
      announceServerDown();
    }
  }

  /**
   * Raw parsing function
   * @return Returns the result of the parse.
   */
  public String parseRaw(String code, String outputType)
    throws NoResponseException, ParseError {
    AldaRequest req = new AldaRequest(host, port);
    req.command = "parse";
    req.body = code;
    req.options = new AldaRequestOptions();
    req.options.output = outputType;
    AldaResponse res = req.send();

    if (!res.success) {
      throw new ParseError(res.body);
    }

    return res.body;
  }

  public void parse(String code, String outputType)
    throws NoResponseException, ParseError {
    String res = parseRaw(code, outputType);
    if (res != null) {
      System.out.println(res);
    }
  }

  public void parse(File file, String outputType)
    throws NoResponseException, ParseError, SystemException {
    String fileBody = Util.readFile(file);
    parse(fileBody, outputType);
  }
}
