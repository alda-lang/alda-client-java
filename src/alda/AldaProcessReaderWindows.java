package alda;

import alda.error.SystemException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AldaProcessReaderWindows implements IAldaProcessReader {
    private static final String ALDA_TASK_NAME = "alda.exe";
    private static final String TASKLIST_COMMAND =
            //"tasklist.exe /fo csv /nh /fi \"/IMAGENAME eq "+ ALDA_TASK_NAME + "\"";
            "WMIC path win32_process where \"Caption='alda.exe'\" get Caption,Commandline,Processid /FORMAT:csv";
    private static final int TASKLIST_COMMAND_N_EXPECTED_COLS = 4;  // Amount of columns expected to be returned by executing TASKLIST_COMMAND
    private static final int TASKLIST_COL_TASKCMD = 2;  // 0 <= this < TASKLIST_COMMAND_N_EXPECTED_COLS
    private static final int TASKLIST_COL_PID = 3;      // 0 <= this < TASKLIST_COMMAND_N_EXPECTED_COLS

    @Override
    public List<AldaProcess> getProcesses() throws SystemException {
        List<AldaProcess> processes = new ArrayList<>();

        try {
            Process p = Runtime.getRuntime().exec(TASKLIST_COMMAND);
            InputStreamReader isr = new InputStreamReader(p.getInputStream());

            BufferedReader input = new BufferedReader(isr);
            String line;
            while ((line = input.readLine()) != null) {
                if (line.contains("alda-fingerprint")) {
                    AldaProcess process = new AldaProcess();
                    String[] columns = line.split(",");
                    if(columns.length != TASKLIST_COMMAND_N_EXPECTED_COLS) {
                        throw new RuntimeException("Got "+ columns.length +" columns: expected "+ TASKLIST_COMMAND_N_EXPECTED_COLS);
                    }
                    process.pid = Integer.parseInt(columns[TASKLIST_COL_PID]);
                    // Extract task parameters
                    String taskCmd = columns[TASKLIST_COL_TASKCMD];
                    Matcher b = Pattern.compile(".*--port (\\d+).*").matcher(line);
                    Matcher c = Pattern.compile(".* server.*").matcher(line);
                    Matcher d = Pattern.compile(".* worker.*").matcher(line);
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

                    processes.add(process);
                }
            }
            input.close();
            p.getInputStream().close();
            p.getOutputStream().close();
            p.getErrorStream().close();
            p.destroy();
            return processes;
        } catch (IOException e) {
            throw new SystemException("Unable to list running processes.", e);
        }
    }
}
