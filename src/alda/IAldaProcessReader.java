package alda;

import alda.error.SystemException;
import java.util.List;

public interface IAldaProcessReader {
    List<AldaProcess> getProcesses() throws SystemException;
}
