package alda;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;

import com.jcabi.manifests.Manifests;

import java.io.BufferedReader;
import java.io.File;
import java.nio.file.Files;
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

  public static void updateAlda() throws URISyntaxException {
    // Get the path to the current alda executable
    String programPath = Util.getProgramPath();
    String latestApiStr = "https://api.github.com/repos/alda-lang/alda/releases/latest";
    String apiResult;
    String clientVersion = version();

    File aldaPath = new File(programPath);
    // Check to see if our programPath is valid
    if (!aldaPath.isFile()) {
      System.err.println("Unable to determine the current location of the alda binary. ");
      System.err.println("Are you running a development version?");
      System.err.println("Attempted location is '" + programPath + "'");
      return;
    }

    // In order to avoid copying directly over the current file, we move our file to a tmp dir,
    // then download where we want (the old location).
    File aldaUpdateTempDir;
    try {
      aldaUpdateTempDir = Files.createTempDirectory("alda-updater").toFile();
    } catch (IOException e) {
      System.err.println("Could not create a temporary directory for the updater.");
      System.err.println("Maybe you are out of space?");
      e.printStackTrace();
      return;
    }
    File aldaCopyPath = new File(
      aldaUpdateTempDir,
      (SystemUtils.IS_OS_WINDOWS ? "alda_old.exe" : "alda_old"));

    // Make a call to the Github API to get the latest version number/download URL
    try {
      apiResult = Util.makeApiCall(latestApiStr);
    } catch (IOException e) {
      System.err.println("There was an error connecting to the Github API.");
      e.printStackTrace();
      return;
    }

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
      System.err.println("Alda download link not found for your platform.");
      return;
    }

    // Request confirmation from user:
    System.out.print("Install alda '" + latestTag + "' over '" + clientVersion + "' ? [yN]: ");
    System.out.flush();
    String name = (new Scanner(System.in)).nextLine();
    if (!(name.equalsIgnoreCase("y") || name.equalsIgnoreCase("yes"))) {
      System.out.println("Quitting...");
      return;
    }

    // Copy existing alda binary to temp directory
    if (!Util.moveFile(aldaPath, aldaCopyPath))
      return;


    System.out.println("Downloading " + downloadURL + "...");

    try {
      // Download file from downloadURL to programPath
      Util.downloadFile(downloadURL, programPath);
    } catch (IOException e) {
      System.err.println("Error while downloading file:");
      e.printStackTrace();
      // Try to get our old file back on the path!
      // Wish us luck...
      System.out.println("Attempting to restore old alda executable.");
      if (!Util.moveFile(aldaCopyPath, aldaPath)) {
        System.err.println("[ERROR] There was an error restoring your old alda installation!");
        System.err.println("You will probably need to reinstall alda completely.");
      }
      return;
    }

    // set as executable if on UNIX
    if (SystemUtils.IS_OS_UNIX) {
      aldaPath.setExecutable(true);
    }

    System.out.println();
    System.out.println("Updated alda " + clientVersion + " => " + latestTag + ".");
    System.out.println("If you have any currently running servers, you may want to restart them so that they are running the latest version.");
  }



  public static void listProcesses(int timeout) throws IOException {

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

  public static boolean checkForExistingServer(int port) throws IOException {
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
