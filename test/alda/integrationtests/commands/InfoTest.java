package alda.integrationtests.commands;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import alda.AldaServer;
import alda.AldaServerOptions;
import alda.repl.commands.ReplCommand;
import alda.repl.commands.ReplCommandManager;
import alda.testutils.AldaServerInfo;
import alda.testutils.TestEnvironment;
import alda.testutils.TestEnvironmentStatus;

/*
 * The test environment is created before, and teared down after,
 * *all* the tests, by the class Alda.testutils.AldaJunitRunListener
 */
public class InfoTest {

    private static final String CMD_INFO = "info";
    private final Map<StringBuffer, Pattern[]> TEST_INPUT_OUTPUT_DATA;

    // Constructor inits input data
    public InfoTest() {
      TEST_INPUT_OUTPUT_DATA = new HashMap<>();
      TEST_INPUT_OUTPUT_DATA.put(
          new StringBuffer(),
          new Pattern[]{
              Pattern.compile(Pattern.quote("Instruments: (none)")),
              Pattern.compile(Pattern.quote("Current instruments: (none)")),
              Pattern.compile(Pattern.quote("Events: 0")),
              Pattern.compile(Pattern.quote("Markers: start"))}
          );

      TEST_INPUT_OUTPUT_DATA.put(
          new StringBuffer("violin \"violin-1\": o4 f2   g4 a   b-2   a " +
              "violin \"violin-2\": o4 c2   e4 f   f2    f " +
              "viola: o3 a2 > c4 c   d2    c " +
              "cello: (volume 75) o3 f2   c4 f < b-2 > f"),
          new Pattern[]{
              Pattern.compile("Instruments: violin-\\S{5,6}, violin-\\S{5,6}, viola-\\S{5,6}, cello-\\S{5,6}"),
              Pattern.compile("Current instruments: cello-\\S{5,6}"),
              Pattern.compile(Pattern.quote("Events: 20")),
              Pattern.compile(Pattern.quote("Markers: start"))}
          );

      TEST_INPUT_OUTPUT_DATA.put(
          new StringBuffer("piano: (tempo 93) c8 d e %one f %two g %three a"),
          new Pattern[]{
              Pattern.compile("Instruments: piano-\\S{5,6}"),
              Pattern.compile("Current instruments: piano-\\S{5,6}"),
              Pattern.compile(Pattern.quote("Events: 6")),
              Pattern.compile(Pattern.quote("Markers: start, one, two, three"))}
          );


      TEST_INPUT_OUTPUT_DATA.put(
          new StringBuffer("piano: (tempo 93) c8 d e %one f %two g %three a"),
          new Pattern[]{
              Pattern.compile("Instruments: piano-\\S{5,6}"),
              Pattern.compile("Current instruments: piano-\\S{5,6}"),
              Pattern.compile(Pattern.quote("Events: 6")),
              Pattern.compile(Pattern.quote("Markers: start, one, two, three"))}
          );
    }

    @Test
    public void listProcessesOutput() throws Exception {
     List<AldaServerInfo> servers = TestEnvironment.getRunningServers();
     assertNotNull("Test environment servers list shall not be null", servers);
     assertTrue("Test environment servers list shall contain at least one server", servers.size() > 0);

     AldaServerInfo server0Info = servers.get(0);

     AldaServerOptions serverOpts = new AldaServerOptions();
     serverOpts.host = server0Info.getHost();
     serverOpts.port = server0Info.getPort();
     serverOpts.timeout = 30;

     AldaServer server = new AldaServer(serverOpts);

     //AldaRepl repl = new AldaRepl(server, globalOpts.verbose);
     ReplCommandManager cmdManager = new ReplCommandManager();
     ReplCommand infoCmd = cmdManager.get(CMD_INFO);
     assertNotNull("Command manager did not return an instance when querying for command '"+ CMD_INFO +"'", infoCmd);
     ReplCommandExecutor cmdExecutor = new ReplCommandExecutor(infoCmd);

     try {
       int nQuery = 1;
       for(Map.Entry<StringBuffer, Pattern[]> entry : TEST_INPUT_OUTPUT_DATA.entrySet()) {
         Optional<String> result = cmdExecutor.executeReplCommandWithExpBackoff(null, entry.getKey(), server, null, null);
         assertTrue("Command result is null", result.isPresent());

         String[] stdOutLines = result.get().split(System.lineSeparator());
         assertEquals("Info command printed the wrong amount of lines for query #"+ nQuery, entry.getValue().length, stdOutLines.length);

         for(int i=0; i<entry.getValue().length; i++) {
           Pattern expectedPattern = entry.getValue()[i];
           Matcher m = expectedPattern.matcher(stdOutLines[i]);
           assertTrue("Info line #"+ i +" did not match the expected string. Expected: "+ expectedPattern.toString() + " but got: "+ stdOutLines[i], m.matches());
         }
         nQuery++;
       }
     } catch(Exception e) {
       System.err.println(e.getMessage());
     }
     //System.out.println(stdOutContent);
    }

}
