package alda;

import java.util.List;

public interface IAldaProcessReader {

    List<AldaProcess> getProcesses() throws alda.error.IOException;
}
