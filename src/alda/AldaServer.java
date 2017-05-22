package alda;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.SystemUtils;

import org.fusesource.jansi.AnsiConsole;
import static org.fusesource.jansi.Ansi.*;
import static org.fusesource.jansi.Ansi.Color.*;

public class AldaServer extends AldaProcess {
  private static int STARTUP_RETRY_INTERVAL = 250; // ms
  private static int PLAY_STATUS_INTERVAL = 250; // ms

  public AldaServer(String host, int port, int timeout, boolean verbose, boolean quiet) {
    this.host = normalizeHost(host);
    this.port = port;
    this.timeout = timeout;
    this.verbose = verbose;
    this.quiet = quiet;

    AnsiConsole.systemInstall();
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

  public void setQuiet(boolean q) {
    this.quiet = q;
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
    prefix = String.format("[%s] ", ansi().fg(BLUE)
                                          .a(prefix)
                                          .reset()
                                          .toString());

    System.out.println(prefix + message);
  }

  public void error(String msg) {
    error(msg, true);
  }

  public void error(String message, boolean catchExceptions) {
    if (!catchExceptions) {
      throw new ServerRuntimeError(message);
    } else {
      String prefix = ansi().fg(RED).a("ERROR ").reset().toString();
      // save and restore quiet value to print out errors
      boolean oldQuiet = quiet;
      quiet = false;
      msg(prefix + message);
      quiet = oldQuiet;
    }
  }

  private final String CHECKMARK = "\u2713";
  private final String X = "\u2717";

  private void ready() {
    msg(ansi().a("Ready ").fg(GREEN).a(CHECKMARK).reset().toString());
  }

  private void serverUp() {
    msg(ansi().a("Server up ").fg(GREEN).a(CHECKMARK).reset().toString());
  }

  private void serverDown(boolean isGood) {
    Color color = isGood ? GREEN : RED;
    String glyph = isGood ? CHECKMARK : X;
    msg(ansi().a("Server down ").fg(color).a(glyph).reset().toString());
  }

  private void serverDown() {
    serverDown(false);
  }

  public void upBg(int numberOfWorkers)
    throws InvalidOptionsException, NoResponseException {
    assertNotRemoteHost();

    boolean serverAlreadyUp = checkForConnection();
    if (serverAlreadyUp) {
      msg("Server already up.");
      System.exit(1);
    }

    boolean serverAlreadyTryingToStart;
    try {
      serverAlreadyTryingToStart = SystemUtils.IS_OS_UNIX &&
                                   AldaClient.checkForExistingServer(this.port);
    } catch (IOException e) {
      System.out.println("WARNING: Unable to detect whether or not there is " +
                         "already a server running on that port.");
      serverAlreadyTryingToStart = false;
    }

    if (serverAlreadyTryingToStart) {
      msg("There is already a server trying to start on this port. Please " +
          "be patient -- this can take a while.");
      System.exit(1);
    }

    Object[] opts = {"--host", host,
                     "--port", Integer.toString(port),
                     "--workers", Integer.toString(numberOfWorkers),
                     "--alda-fingerprint"};

    try {
      Util.forkProgram(Util.conj(opts, "server"));
      msg("Starting Alda server...");

      boolean serverUp = waitForConnection();
      if (serverUp) {
        serverUp();
      } else {
        serverDown();
        return;
      }
    } catch (URISyntaxException e) {
      error(String.format("Unable to fork '%s' into the background; " +
            " got URISyntaxException: %s", e.getInput(), e.getReason()));
    } catch (IOException e) {
      error(String.format("An IOException occurred trying to fork a background process: %s",
            e.getMessage()));
    }

    msg("Starting worker processes...");

    int workersAvailable = 0;
    while (workersAvailable == 0) {
      try {
        Thread.sleep(STARTUP_RETRY_INTERVAL);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        System.out.println("Thread interrupted.");
        return;
      }
      AldaRequest req = new AldaRequest(this.host, this.port);
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

    ready();
  }

  public void upFg(int numberOfWorkers) throws InvalidOptionsException {
    assertNotRemoteHost();

    Object[] args = {numberOfWorkers, port, verbose};

    Util.callClojureFn("alda.server/start-server!", args);
  }

  // TODO: rewrite REPL as a client that communicates with a server
  public void startRepl() throws InvalidOptionsException {
    assertNotRemoteHost();

    Object[] args = {};

    Util.callClojureFn("alda.repl/start-repl!", args);
  }

  public void down() throws NoResponseException {
    boolean serverAlreadyDown = !checkForConnection();
    if (serverAlreadyDown) {
      msg("Server already down.");
      return;
    }

    msg("Stopping Alda server...");

    AldaRequest req = new AldaRequest(this.host, this.port);
    req.command = "stop-server";

    try {
      AldaResponse res = req.send();
      if (res.success) {
        serverDown(true);
      } else {
        throw new NoResponseException("Failed to stop server.");
      }
    } catch (NoResponseException e) {
      serverDown(true);
    }
  }

  public void downUp(int numberOfWorkers)
    throws NoResponseException, InvalidOptionsException {
    down();
    System.out.println();
    upBg(numberOfWorkers);
  }

  public void status() {
    AldaRequest req = new AldaRequest(this.host, this.port);
    req.command = "status";

    try {
      AldaResponse res = req.send();

      if (res.success) {
        msg(res.body);
      } else {
        error(res.body);
      }
    } catch (NoResponseException e) {
      serverDown();
    }
  }

  public void version() throws NoResponseException {
    AldaRequest req = new AldaRequest(this.host, this.port);
    req.command = "version";
    AldaResponse res = req.send();
    String serverVersion = res.body;

    msg(serverVersion);
  }

  public AldaResponse play(String code, String from, String to)
    throws NoResponseException{
    return play(code, null, from, to);
  }

  public AldaResponse play(String code, String history, String from, String to)
    throws NoResponseException {
    return play(code, history, from, to, true);
  }

  /**
   * Tries to play a bit of alda code
   *
   * @param code The pimary code to play
   * @param history The history context to supplement code
   * @param from Time to play from
   * @param to Time to stop playing
   * @param catchExceptions Whether this method should catch it's exceptions
   * @return The response from the play, with usefull information. Null if we encountered an error.
   */
  public AldaResponse play(String code, String history, String from, String to, boolean catchExceptions)
    throws NoResponseException {

    AldaRequest req = new AldaRequest(this.host, this.port);
    req.command = "play";
    req.body = code;
    req.options = new AldaRequestOptions();
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
      error(res.body, catchExceptions);
      return null;
    }

    if (res.workerAddress == null) {
      error("No worker address included in response; unable to check for status.", catchExceptions);
      return null;
    }

    String status = "requested";

    while (true) {
      AldaResponse update = playStatus(res.workerAddress);
      if (!update.success) {
        error(update.body, catchExceptions);
        break;
      } else if (!update.body.equals(status)) {
        status = update.body;
        switch (status) {
          case "parsing": msg("Parsing/evaluating..."); break;
          case "playing": msg("Playing..."); break;
          default: msg(status);
        }
      }

      if (update.pending) {
        try {
          Thread.sleep(PLAY_STATUS_INTERVAL);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          System.out.println("Thread interrupted.");
        }
      } else {
        // We succeded!
        return update;
      }
    }
    // We don't have anything to return,
    return null;
  }

  public void play(File file, String from, String to)
    throws NoResponseException {
    try {
      String fileBody = Util.readFile(file);
      play(fileBody, from, to);
    } catch (IOException e) {
      error("Unable to read file: " + file.getAbsolutePath());
    }
  }

  public AldaResponse playStatus(byte[] workerAddress)
    throws NoResponseException {
    AldaRequest req = new AldaRequest(this.host, this.port);
    req.command = "play-status";
    req.workerToUse = workerAddress;
    return req.send();
  }

  /**
   * Raw parsing function
   * @return Returns the result of the parse, or null if the parse failed (and no exception was thrown)
   */
  public String parseRaw(String code, String mode, boolean parseExceptions) throws NoResponseException {
    AldaRequest req = new AldaRequest(this.host, this.port);
    req.command = "parse";
    req.body = code;
    req.options = new AldaRequestOptions();
    req.options.as = mode;
    AldaResponse res = req.send();

    if (res.success) {
      return res.body;
    } else {
      error(res.body, parseExceptions);
      return null;
    }
  }

  public void parse(String code, String mode) throws NoResponseException {
    String res = parseRaw(code, mode, true);
    if (res != null) {
      System.out.println(res);
    }
  }

  public void parse(File file, String mode) throws NoResponseException {
    try {
      String fileBody = Util.readFile(file);
      parse(fileBody, mode);
    } catch (IOException e) {
      error("Unable to read file: " + file.getAbsolutePath());
    }
  }
}
