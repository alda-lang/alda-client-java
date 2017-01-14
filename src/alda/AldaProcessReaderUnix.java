package alda;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.SystemUtils;

public class AldaProcessReaderUnix implements IAldaProcessReader {

    private static String PROCESS_LIST_BSD_COMMAND = "ps -ax";
    private static String PROCESS_LIST_ATANDT_COMMAND = "ps ax";

    @Override
    public List<AldaProcess> getProcesses() throws IOException {
        List<AldaProcess> processes = new ArrayList<AldaProcess>();

        String ps;
        if (isBsdPsCommand()){
            ps = PROCESS_LIST_BSD_COMMAND;
        } else {
            ps = PROCESS_LIST_ATANDT_COMMAND;
        }
        Process p = Runtime.getRuntime().exec(ps);
        InputStreamReader isr = new InputStreamReader(p.getInputStream());
        BufferedReader input = new BufferedReader(isr);
        String line;
        while ((line = input.readLine()) != null) {
            if (line.contains("alda-fingerprint")) {
                AldaProcess process = new AldaProcess();

                Matcher a = Pattern.compile("^\\s*(\\d+).*").matcher(line);
                Matcher b = Pattern.compile(".*--port (\\d+).*").matcher(line);
                Matcher c = Pattern.compile(".* server.*").matcher(line);
                Matcher d = Pattern.compile(".* worker.*").matcher(line);
                if (a.find()) {
                    process.pid = Integer.parseInt(a.group(1));
                    if (b.find()) {
                        process.port = Integer.parseInt(b.group(1));
                    } else {
                        process.port = -1;
                    }

                    if (c.find()) {
                        process.type = "server";
                    }

                    if (d.find()) {
                        process.type = "worker";
                    }
                }

                processes.add(process);
            }
        }
        input.close();
        p.getInputStream().close();
        p.getOutputStream().close();
        p.getErrorStream().close();
        p.destroy();
        return processes;
    }

    private boolean isBsdPsCommand() {
        return SystemUtils.IS_OS_MAC_OSX || SystemUtils.IS_OS_FREE_BSD || SystemUtils.IS_OS_NET_BSD || SystemUtils.IS_OS_OPEN_BSD;
    }

}
