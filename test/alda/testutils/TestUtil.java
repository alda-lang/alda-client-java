package alda.testutils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class TestUtil {
  public static String randomTempFilename(String tmpdirname)
    throws IOException {
    Path tmpdir = Files.createTempDirectory(tmpdirname);
    String filename = UUID.randomUUID().toString();
    Path tmpfile = Paths.get(tmpdir.toString(), filename);
    return tmpfile.toAbsolutePath().toString();
  }
}
