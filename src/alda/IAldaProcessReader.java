package alda;

import java.io.IOException;
import java.util.List;

public interface IAldaProcessReader {

    List<AldaProcess> getProcesses() throws IOException;
}
