package alda;

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
      socket.connect(this.host + ":" + this.port);
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
  private final static int WRONG_JOB_RETRIES = 10;

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

  /**
   * Try up to WRONG_JOB_RETRIES times to receive a message from the server that
   * has the same jobId as the request we sent.
   *
   * This is to avoid accidentally using a response that was sent out of sync
   * for a different request.
   *
   * Returns null if we fail to receive a message for our jobId.
   */
  private AldaResponse receiveMatchingResponse(Socket client, int timeout)
    throws NoResponseException {
    Poller poller = getPoller();

    String jobId = options == null ? null : options.jobId;

    int retries = WRONG_JOB_RETRIES;

    while (retries > 0) {
      int rc = poller.poll(timeout);
      if (rc == -1) {
        throw new NoResponseException("Connection interrupted.");
      }

      if (poller.pollin(0)) {
        ZMsg zmsg = ZMsg.recvMsg(client);
        if (debug) zmsg.dump();

        byte[] address = zmsg.unwrap().getData(); // discard envelope
        String responseJson = zmsg.popString();

        AldaResponse response = AldaResponse.fromJson(responseJson);

        if (jobId != null &&
            response.jobId != null &&
            !response.jobId.equals(jobId)) {
          retries--;
          continue;
        }

        if (!response.noWorker)
          response.workerAddress = zmsg.pop().getData();

        return response;
      }

      retries--;
    }

    return null;
  }

  private AldaResponse sendRequest(String req, int timeout, int retries)
    throws NoResponseException {
    if (retries < 0 || Thread.currentThread().isInterrupted()) {
      throw new NoResponseException("Alda server is down. To start the server, run `alda up`.");
    }

    Poller poller = getPoller();
    Socket client = rebuildSocket();

    ZMsg msg = new ZMsg();

    msg.addString(req);
    if (this.workerToUse != null) {
      msg.add(this.workerToUse);
    }

    msg.addString(this.command);

    msg.send(client);
    AldaResponse response = receiveMatchingResponse(client, timeout);

    if (response != null)
      return response;

    // Didn't get a response within the allowed timeout. Rebuild the socket and
    // try sending the request again, unless we're out of retries.
    return sendRequest(req, timeout, retries - 1);
  }

  private AldaResponse sendRequest(String req, int timeout)
    throws NoResponseException {
    return sendRequest(req, timeout, REQUEST_RETRIES);
  }

  private AldaResponse sendRequest(String req)
    throws NoResponseException {
    return sendRequest(req, REQUEST_TIMEOUT, REQUEST_RETRIES);
  }

  public AldaResponse send(int timeout, int retries)
    throws NoResponseException {
    return sendRequest(this.toJson(), timeout, retries);
  }

  public AldaResponse send(int timeout) throws NoResponseException {
    return send(timeout, REQUEST_RETRIES);
  }

  public AldaResponse send() throws NoResponseException {
    return send(REQUEST_TIMEOUT, REQUEST_RETRIES);
  }
}
