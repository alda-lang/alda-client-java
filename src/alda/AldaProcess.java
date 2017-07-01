package alda;

import alda.error.NoResponseException;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class AldaProcess {
  private static int PING_TIMEOUT = 100; // ms
  private static int PING_RETRIES = 5;
  private static int STARTUP_RETRY_INTERVAL = 250; // ms

  public boolean verbose = false;
  public boolean quiet = false;
  public String host;
  public int pid;
  public int port;
  public String type;
  public int timeout;

  public boolean checkForConnection(int timeout, int retries) {
    try {
      AldaRequest req = new AldaRequest(this.host, this.port);
      req.command = "ping";
      AldaResponse res = req.send(timeout, retries);
      return res.success;
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
    // Calculate the number of retries before giving up, based on the fixed
    // STARTUP_RETRY_INTERVAL and the desired timeout in seconds.
    int retriesPerSecond = 1000 / this.STARTUP_RETRY_INTERVAL;
    int retries = this.timeout * retriesPerSecond;

    if (!checkForConnection(this.STARTUP_RETRY_INTERVAL, retries)) {
      throw new NoResponseException(
        "Timed out waiting for response from the server."
      );
    }
  }
}
