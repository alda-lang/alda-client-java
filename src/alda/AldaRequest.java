package alda;

import alda.error.NoResponseException;

import com.google.gson.Gson;

import org.zeromq.ZContext;
import org.zeromq.ZMsg;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.PollItem;
import org.zeromq.ZMQ.Poller;
import org.zeromq.ZMQ.Socket;

public class AldaRequest {
  private static ZContext zContext = null;
  public static ZContext getZContext() {
    if (zContext == null) {
      zContext = new ZContext();
    }
    return zContext;
  }

  private static Poller poller;
  private Poller getPoller() {
    if (poller == null) {
      poller = getZContext().createPoller(1);
    }
    return poller;
  }

  private static Socket socket;
  private Socket getSocket() {
    if (socket == null) {
      socket = getZContext().createSocket(ZMQ.DEALER);
      socket.connect(host + ":" + port);
      getPoller().register(socket, Poller.POLLIN);
    }
    return socket;
  }

  private void destroySocket() {
    if (socket != null) {
      poller.unregister(socket);
      getZContext().destroySocket(socket);
      socket = null;
    }
  }

  private Socket rebuildSocket() {
    destroySocket();
    return getSocket();
  }

  private final static int REQUEST_TIMEOUT = 500; //  ms
  private final static int REQUEST_RETRIES = 10;  //  Before we abandon

  // Enable debug to print out all json queries to server
  private static boolean debug = false;

  private transient String host;
  private transient int port;
  public transient byte[] workerToUse;

  public AldaRequest(String host, int port) {
    this.host = host;
    this.port = port;
  }

  public String command;
  public String body;
  public AldaRequestOptions options;

  /**
   * Sets the global debug flag on/off to print all incoming json
   */
  public static void setDebug(boolean toSet) {
    debug = toSet;
  }
  /**
   * Gets the global debug flag, see setDebug
   */
  public static boolean getDebug() {
    return debug;
  }

  public String toJson() {
    Gson gson = new Gson();
    return gson.toJson(this);
  }

  private AldaResponse sendRequest(ZMsg request, int timeout, int retries)
    throws NoResponseException {
    // When non-null, used to help ensure that we don't use a response from the
    // server that was for a different request.
    String jobId = options == null ? null : options.jobId;

    while (retries >= 0 && !Thread.currentThread().isInterrupted()) {
      Poller poller = getPoller();
      Socket client = rebuildSocket();

      // false means don't destroy the message after sending
      request.send(client, false);

      int rc = poller.poll(timeout);
      if (rc == -1) {
        request.destroy();
        throw new NoResponseException("Connection interrupted.");
      }

      if (poller.pollin(0)) {
        ZMsg zmsg = ZMsg.recvMsg(client);
        if (debug) zmsg.dump();

        byte[] address = zmsg.unwrap().getData(); // discard envelope
        String responseJson = zmsg.popString();

        AldaResponse response = AldaResponse.fromJson(responseJson);

        // If there is a jobId option, we will ignore any response from the
        // server that doesn't have the same jobId, and try again. This will not
        // count against our remaining retries, as the server did respond.
        if (jobId != null &&
            response.jobId != null &&
            !jobId.equals(response.jobId))
          continue;

        if (!response.noWorker)
          response.workerAddress = zmsg.pop().getData();

        request.destroy();
        return response;
      }

      // Didn't get a response within the allowed timeout. Try again, unless
      // we're out of retries.
      retries--;
    }

    request.destroy();
    String errorMsg = "Alda server is down. To start the server, run `alda up`.";
    throw new NoResponseException(errorMsg);
  }

  public AldaResponse send(int timeout, int retries) throws NoResponseException {
    ZMsg request = new ZMsg();

    request.addString(this.toJson());

    if (workerToUse != null) {
      request.add(workerToUse);
    }

    request.addString(command);

    return sendRequest(request, timeout, retries);
  }

  public AldaResponse send(int timeout) throws NoResponseException {
    return send(timeout, REQUEST_RETRIES);
  }

  public AldaResponse send() throws NoResponseException {
    return send(REQUEST_TIMEOUT, REQUEST_RETRIES);
  }
}
