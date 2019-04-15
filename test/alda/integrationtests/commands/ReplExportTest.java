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
import alda.testutils.TestEnvironment;
import alda.testutils.TestEnvironmentStatus;
import alda.testutils.TestUtil;

public class ReplExportTest {
    private static final String CMD_EXPORT = "export";

    public ReplExportTest() {}

    @Test
    public void exportMidiFile() throws Exception {
        ReplCommandManager cmdManager = new ReplCommandManager();
        ReplCommand exportCmd = cmdManager.get(CMD_EXPORT);

        assertNotNull(
            "Command manager did not return an instance when querying for " +
            "command '"+ CMD_EXPORT +"'", exportCmd
        );

        ReplCommandExecutor cmdExecutor = new ReplCommandExecutor(exportCmd);

        AldaServer server = TestEnvironment.getServer();

        StringBuffer history = new StringBuffer(
            "(quant! 95)\n" +
            "violin \"vn1\": o4 a2 > c < b1~4\n" +
            "violin \"vn2\": o4 d2   d   d1~4\n" +
            "viola:          o3 f+2  a   g1~4\n" +
            "cello:          o3 d2 < d   g1~4\n"
        );

        String filename = TestUtil.randomTempFilename("repl-export-test");

        Optional<String> result =
            cmdExecutor.executeReplCommandWithExpBackoff(
                filename, history, server, null, null
            );

        assertTrue("Command result is null", result.isPresent());
    }
}
