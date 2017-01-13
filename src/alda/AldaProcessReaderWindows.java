package alda;

import java.util.ArrayList;
import java.util.List;

public class AldaProcessReaderWindows implements IAldaProcessReader {
    @Override
    public List<AldaProcess> getProcesses() {

        System.out.println("Sorry -- listing running processes is not " +
                "currently supported for Windows users.");

        return new ArrayList<>(0);
    }
}
