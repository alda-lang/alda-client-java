package alda.testutils;

import alda.error.SystemException;
import alda.AldaServer;
import alda.AldaServerOptions;

import java.io.*;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

public class TestEnvironment {

    private static final String ALDA_EXEC_ENVIRONMENT_VAR = "ALDA_EXECUTABLE";
    private static final String ALDA_EXEC_INSTALL_PATH_UNIX = "/usr/local/bin/alda";
    private static final String ALDA_EXEC_FALLBACK = "test/resources/server/alda";

    private static TestEnvironmentStatus STATUS = TestEnvironmentStatus.STOPPED;
    private static List<AldaServerInfo> RUNNING_SERVERS;
    private static String ALDA_EXECUTABLE;
    public static int NO_SERVER_RUNNING_PORT;

    public static void setUp() throws Exception {

        STATUS = TestEnvironmentStatus.STARTING;

        ALDA_EXECUTABLE = findAldaExecutable();
        NO_SERVER_RUNNING_PORT = findOpenPort();

        // Start 2 Servers with different number of workers
        RUNNING_SERVERS  = new ArrayList<>(2);
        RUNNING_SERVERS.add(new AldaServerInfo("localhost", 0, 2));
        RUNNING_SERVERS.add(new AldaServerInfo("localhost", 0, 1));

        for (AldaServerInfo srv: RUNNING_SERVERS){
            srv.setPort(findOpenPort());
            // We are also checking for correct number of workers, so stop possible running instances
            TestEnvironment.stopAldaServer(srv.getPort());
            // start server on port with number of workers
            TestEnvironment.startAldaServerIfNotRunning(srv.getPort(), srv.getNumberOfWorkers());
        }

        // ensure that there is no server running on NO_SERVER_RUNNING_PORT
        TestEnvironment.stopAldaServer(NO_SERVER_RUNNING_PORT);

        STATUS = TestEnvironmentStatus.STARTED;
    }

    private static String findAldaExecutable() {

        // 1. check environment variable
        if (System.getenv(ALDA_EXEC_ENVIRONMENT_VAR) != null) {
            return System.getenv(ALDA_EXEC_ENVIRONMENT_VAR);
        }
        // 2. check default alda *nix install path
        if (new File(ALDA_EXEC_INSTALL_PATH_UNIX).exists()){
            return ALDA_EXEC_INSTALL_PATH_UNIX;
        }

        // 3. default fallback
        return ALDA_EXEC_FALLBACK;
    }

    public static void tearDown() throws Exception {
        for (AldaServerInfo srv: RUNNING_SERVERS){
            TestEnvironment.stopAldaServer(srv.getPort());
        }
        STATUS = TestEnvironmentStatus.STOPPED;
    }

    public static List<AldaServerInfo> getRunningServers() {
        return RUNNING_SERVERS;
    }

    public static TestEnvironmentStatus getStatus() {
        return STATUS;
    }

    public static AldaServer getServer() {
        assert(RUNNING_SERVERS != null);
        assert(RUNNING_SERVERS.size() > 0);

        AldaServerInfo server0Info = RUNNING_SERVERS.get(0);

        AldaServerOptions serverOpts = new AldaServerOptions();
        serverOpts.host = server0Info.getHost();
        serverOpts.port = server0Info.getPort();
        serverOpts.timeout = 30;

        return new AldaServer(serverOpts);
    }

    private static void startAldaServerIfNotRunning(int port, int numberOfWorkers)
    throws SystemException {
      try {
        Process p = Runtime.getRuntime().exec(ALDA_EXECUTABLE+" -p "+port+" -w "+numberOfWorkers+" up");
        printProcessOutput(p.getInputStream());
      } catch (IOException e) {
        throw new SystemException("Unable to start Alda server.", e);
      }
    }

    private static void stopAldaServer(int port) {
        try {
            Process p = Runtime.getRuntime().exec(ALDA_EXECUTABLE+" -p "+port+" down");
            printProcessOutput(p.getInputStream());
        } catch (IOException ignore) {}

    }

    private static void printProcessOutput(InputStream inputStream)
    throws IOException {
        InputStreamReader isr = new InputStreamReader(inputStream);
        BufferedReader input = new BufferedReader(isr);
        String line;
        while ((line = input.readLine()) != null) {
            System.out.println(line);
        }
    }

    public static int findOpenPort() throws IOException {
        ServerSocket tmpSocket = new ServerSocket(0);
        int portNumber = tmpSocket.getLocalPort();
        tmpSocket.close();
        return portNumber;
    }

}
