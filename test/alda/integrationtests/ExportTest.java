package alda.integrationtests;

import static org.junit.Assert.*;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;

import org.junit.Test;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import alda.AldaServer;
import alda.testutils.TestEnvironment;
import alda.testutils.TestEnvironmentStatus;
import alda.testutils.TestUtil;

public class ExportTest {
    public ExportTest() {}

    @Test
    public void exportMidiFile() throws Exception {
        AldaServer server = TestEnvironment.getServer();
        String filename = TestUtil.randomTempFilename("export-test");
        server.export("piano: c8 d e f g2", "midi", filename);

        File file = new File(filename);
        assertTrue("Exported MIDI file does not exist.", file.exists());

        Sequence sequence = MidiSystem.getSequence(file);
        // see: https://stackoverflow.com/q/5686755/2338327
        double epsilon = 0;
        assertEquals(Sequence.PPQ, sequence.getDivisionType(), epsilon);
        assertEquals(128, sequence.getResolution(), epsilon);
    }
}

