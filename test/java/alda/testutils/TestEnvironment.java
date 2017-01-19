package alda.testutils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

public class TestEnvironment {

    private static TestEnvironmentStatus STATUS = TestEnvironmentStatus.STOPPED;
    private static List<AldaServerInfo> RUNNING_SERVERS;
    private static final String ALDA_EXECUTABLE = "test/resources/client/alda";

    public static int NO_SERVER_RUNNING_PORT;

    public static void setUp() throws Exception {

        STATUS = TestEnvironmentStatus.STARTING;

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

    private static void startAldaServerIfNotRunning(int port, int numberOfWorkers ) throws IOException{
        Process p = Runtime.getRuntime().exec(ALDA_EXECUTABLE+" -p "+port+" -w "+numberOfWorkers+" up");
        printProcessOutput(p.getInputStream());
    }

    private static void stopAldaServer(int port) {
        try {
            Process p = Runtime.getRuntime().exec(ALDA_EXECUTABLE+" -p "+port+" down");
            printProcessOutput(p.getInputStream());
        } catch (IOException ignore) {}

    }

    private static void printProcessOutput(InputStream inputStream) throws IOException {
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
