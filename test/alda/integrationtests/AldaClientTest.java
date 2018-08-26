package alda.integrationtests;

import alda.AldaClient;
import alda.AldaServerOptions;
import alda.testutils.AldaServerInfo;
import alda.testutils.TestEnvironment;
import alda.testutils.TestEnvironmentStatus;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/*
 * The test environment is created before, and teared down after,
 * *all* the tests, by the class Alda.testutils.AldaJunitRunListener
 */
public class AldaClientTest {

    private final ByteArrayOutputStream stdOutContent = new ByteArrayOutputStream();

    @Test
    public void listProcessesOutput() throws Exception {
        // Redirect StdOut
        PrintStream oldStdOut = System.out;
        System.setOut(new PrintStream(stdOutContent));
        try {
            AldaServerOptions serverOpts = new AldaServerOptions();
            serverOpts.noColor = true;
            serverOpts.timeout = 30;
            AldaClient.listProcesses(serverOpts);

            Map<Integer, Integer> serverPortsFound = new HashMap<>();
            Pattern serverPortPattern = Pattern.compile("\\[(.*?)\\]");
            Pattern serverBackendPortPattern = Pattern.compile("backend port\\:(.*?)\\)");

            String[] stdOutLines = stdOutContent.toString().split("\n");
            for (String line : stdOutLines){
                // Format: [27716] Server up (2/2 workers available, backend port: 39998)
                if (line.contains("Server up")){
                    Matcher sp = serverPortPattern.matcher(line);
                    if (sp.find()){
                        int srvPort = Integer.parseInt(sp.group(1));
                        int numberOfWorkers = 0;

                        Matcher sbp = serverBackendPortPattern.matcher(line);
                        if (sbp.find()){
                            int srvBackendPort = Integer.parseInt(sbp.group(1).trim());
                            numberOfWorkers = parseOutputForNumberOfWorkersOnBackendPort(stdOutLines, srvBackendPort);
                        }

                        serverPortsFound.put(srvPort, numberOfWorkers);
                    }
                }
            }

            for(AldaServerInfo server: TestEnvironment.getRunningServers()) {
                assertTrue("Running server not listed.", serverPortsFound.containsKey(server.getPort()) );
                assertEquals("Number of workers for server don't match.", server.getNumberOfWorkers(), serverPortsFound.get(server.getPort()).intValue() );
            }

        } finally {
            // Reset StdOut
            System.setOut(oldStdOut);
            System.out.println(stdOutContent);
        }

    }

    private int parseOutputForNumberOfWorkersOnBackendPort(String[] lines, int srv_backend_port) {
        int workersFound = 0;
        for (String line : lines) {
            // Format: [46672] Worker (pid: 6368)
            if (line.contains("["+srv_backend_port+"] Worker")) {
                workersFound++;
            }
        }
        return workersFound;
    }

    @Test
    public void testCheckForExistingServer() throws Exception {
        for(AldaServerInfo server: TestEnvironment.getRunningServers()) {
            assertTrue("Alda Client didn't detect running server.", AldaClient.checkForExistingServer(server.getPort()) );
        }
        assertFalse("Alda Client detects running Server on a port where there should be none. ",AldaClient.checkForExistingServer(TestEnvironment.NO_SERVER_RUNNING_PORT));
    }

}
