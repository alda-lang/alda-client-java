package alda;

import alda.error.UnsuccessfulException;
import alda.error.SystemException;
import alda.error.ExitCode;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;

import com.jcabi.manifests.Manifests;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Scanner;

import org.apache.commons.lang3.SystemUtils;

public class AldaClient {
  public static String version() {
    if (Manifests.exists("alda-version")) {
      return Manifests.read("alda-version");
    } else {
      return "unknown / development version";
    }
  }

  public static void updateAlda()
  throws SystemException, UnsuccessfulException {
    // The path to the current alda executable
    String programPath;

    try {
      programPath = Util.getProgramPath();
    } catch (URISyntaxException e) {
      throw new SystemException(
        "Could not determine the path to the current `alda` executable.", e
      );
    }

    String clientVersion = version();

    // Make a call to the Github API to get the latest version number/download URL
    String latestApiStr = "https://api.github.com/repos/alda-lang/alda/releases/latest";
    String apiResult = Util.makeApiCall(latestApiStr);

    // Turn api result into version numbers and links
    Gson gson = new Gson();
    JsonObject job = gson.fromJson(apiResult, JsonObject.class);

    // Gets the download URL. This may have ...alda or ...alda.exe
    String downloadURL = null;
    String dlRegex = SystemUtils.IS_OS_WINDOWS ? ".*alda\\.exe$" : ".*.alda$";
    String latestTag = job.getAsJsonObject().get("tag_name").toString().replaceAll("\"", "");

    // Check to see if we currently have the version determined by latestTag
    if (latestTag.indexOf(clientVersion) != -1 || clientVersion.indexOf(latestTag) != -1) {
      System.out.println("Your version of alda (" + clientVersion +") is up to date!");
      return;
    }

    for (JsonElement i : job.getAsJsonArray("assets")) {
      String candidate = i.getAsJsonObject().get("browser_download_url").toString().replaceAll("\"", "");
      if (candidate.matches(dlRegex)) {
        downloadURL = candidate;
        break;
      }
    }

    if (downloadURL == null) {
      throw new UnsuccessfulException(
        "Alda download link not found for your platform. Please file an " +
        "issue at: https://github.com/alda-lang/alda-client-java/issues/new"
      );
    }

    // Workaround for windows. See #24 #25
    if (SystemUtils.IS_OS_WINDOWS) {
      System.out.println(
        "Automated updates to alda.exe are not possible due to limitations of the Windows OS.\n" +
        "For more information, see: https://github.com/alda-lang/alda-client-java/issues/24\n" +
        "\n" +

        "To update alda.exe, first, make sure that the server is not up by running:\n" +
        "\n" +
        "   alda down\n" +
        "\n" +

        "Then, run the following command in your terminal to download the latest alda.exe:\n" +
        "\n" +
        "    powershell -Command Invoke-WebRequest -Uri \"" + downloadURL + "\" -OutFile \"" + programPath + "\"\n" +
        "\n" +

        "Or, if you'd prefer, you can install the latest alda.exe yourself from:\n" +
        "https://github.com/alda-lang/alda/releases\n"
      );
      ExitCode.SUCCESS.exit();
    }

    // Request confirmation from user:
    System.out.print("Install alda '" + latestTag + "' over '" + clientVersion + "' ? [yN]: ");
    System.out.flush();
    String name = (new Scanner(System.in)).nextLine();
    if (!(name.equalsIgnoreCase("y") || name.equalsIgnoreCase("yes"))) {
      System.out.println("Quitting...");
      return;
    }

    System.out.println("Downloading " + downloadURL + "...");

    // Download file from downloadURL to programPath
    Util.downloadFile(downloadURL, programPath);

    // set as executable if on UNIX
    if (SystemUtils.IS_OS_UNIX) {
      new File(programPath).setExecutable(true);
    }

    System.out.println();
    System.out.println("Updated alda " + clientVersion + " => " + latestTag + ".");
    System.out.println("If you have any currently running servers, you may want to restart them so that they are running the latest version.");
  }



  public static void listProcesses(int timeout) throws SystemException {
    IAldaProcessReader processReader = getProcessReader();
    List<AldaProcess> processes = processReader.getProcesses();

    for (AldaProcess process : processes) {
      if (process.type == "server") {
        if (process.port == -1) {
          System.out.printf("[???] Mysterious server running on unknown " +
                            "port (pid: %d)\n", process.pid);
          System.out.flush();
        } else {
          AldaServer server = new AldaServer("localhost",
                                             process.port,
                                             timeout,
                                             false, false);
          server.status();
        }
      } else if (process.type == "worker") {
        if (process.port == -1) {
          System.out.printf("[???] Mysterious worker running on unknown " +
                            "port (pid: %d)\n", process.pid);
          System.out.flush();
        } else {
          System.out.printf("[%d] Worker (pid: %d)\n", process.port, process.pid);
          System.out.flush();
        }
      } else {
        if (process.port == -1) {
          System.out.printf("[???] Mysterious Alda process running on " +
                            "unknown port (pid: %d)\n", process.pid);
          System.out.flush();
        } else {
          System.out.printf("[%d] Mysterious Alda process (pid: %d)\n",
                            process.port, process.pid);
          System.out.flush();
        }
      }
    }
  }

  private static IAldaProcessReader getProcessReader() {

    if (SystemUtils.IS_OS_WINDOWS) {
      return new AldaProcessReaderWindows();
    } else {
      return new AldaProcessReaderUnix();
    }
  }

  public static boolean checkForExistingServer(int port)
    throws SystemException {
    IAldaProcessReader processReader = getProcessReader();
    List<AldaProcess> processes = processReader.getProcesses();

    for (AldaProcess process : processes) {
      if (process.port == port) {
        return true;
      }
    }

    return false;
  }
}
