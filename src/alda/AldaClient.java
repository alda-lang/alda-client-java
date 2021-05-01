package alda;

import alda.error.NoResponseException;
import alda.error.UnsuccessfulException;
import alda.error.SystemException;
import alda.error.ExitCode;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Scanner;

import org.apache.commons.lang3.SystemUtils;

public class AldaClient {
  private static UnsuccessfulException unexpectedAldaApiResponseError(
      JsonObject json
  ) {
    return new UnsuccessfulException("Unexpected Alda API response: " + json);
  }

  public static void updateAlda()
  throws SystemException, UnsuccessfulException {
    String clientVersion = Util.version();

    String apiResult = Util.makeApiCall(
      "https://api.alda.io/releases/latest?from-version=" + clientVersion
    );

    JsonObject json = new Gson().fromJson(apiResult, JsonObject.class);

    JsonPrimitive explanation = json.getAsJsonPrimitive("explanation");
    if (explanation != null) {
      System.out.println("\n" + Util.repeat(80, "-") + "\n");
      System.out.println(Util.indent(1, explanation.getAsString()));
      System.out.println("\n" + Util.repeat(80, "-") + "\n");
    }

    JsonArray releases = json.getAsJsonArray("releases");
    if (releases == null) {
      throw unexpectedAldaApiResponseError(json);
    }

    if (releases.size() == 0) {
      System.out.println("Your version of Alda (" + clientVersion +") is up to date!");
      return;
    }

    // It's possible for the API to return multiple options to download, e.g. if
    // there is a newer release in the 1.x series than the one installed, AND a
    // newer major version available.
    //
    // We could ask the user to choose which one they want, but to keep this
    // simple, we'll just install the newest one.
    JsonElement r = releases.get(releases.size() - 1);

    if (!r.isJsonObject()) {
      throw unexpectedAldaApiResponseError(json);
    }

    JsonObject release = r.getAsJsonObject();

    String releaseVersion = "<unknown version>";
    JsonPrimitive rv = release.getAsJsonPrimitive("version");
    if (rv != null) {
      releaseVersion = rv.getAsString();
    }

    String releaseDate = "<unknown date>";
    JsonPrimitive rd = release.getAsJsonPrimitive("date");
    if (rd != null) {
      releaseDate = rd.getAsString();
    }

    JsonPrimitive changelog = release.getAsJsonPrimitive("changelog");
    if (changelog != null) {
      System.out.printf("Alda %s (released %s)\n", releaseVersion, releaseDate);
      System.out.println();
      System.out.println(Util.indent(1, changelog.getAsString()));
      System.out.println();
    }

    System.out.printf("You have Alda %s installed.\n", clientVersion);
    System.out.printf("Update to Alda %s? [yN]: ", releaseVersion, releaseDate);
    System.out.flush();
    String name = (new Scanner(System.in)).nextLine();
    if (!(name.equalsIgnoreCase("y") || name.equalsIgnoreCase("yes"))) {
      System.out.println("Quitting...");
      return;
    }

    JsonObject allAssets = release.getAsJsonObject("assets");
    if (allAssets == null) {
      throw unexpectedAldaApiResponseError(json);
    }

    String osAndArch = Util.osAndArch();

    JsonArray releaseAssets = allAssets.getAsJsonArray(osAndArch);
    if (releaseAssets == null) {
      throw new SystemException(
        "Oops! We couldn't find an Alda release for your platform " +
        "(" + osAndArch + "). " +
        "Please file an issue at: " +
        "https://github.com/alda-lang/alda/issues/new/choose"
      );
    }

    Path assetsDir;
    try {
      assetsDir = Files.createTempDirectory("alda-update");
    } catch (IOException e) {
      throw new SystemException("Failed to create temp directory.", e);
    }

    CountDownLatch downloads = new CountDownLatch(releaseAssets.size());
    List<Exception> downloadErrors = new ArrayList<Exception>();

    List<String> assetNames = new ArrayList<String>();
    List<Path> downloadedAssets = new ArrayList<Path>();

    for (JsonElement a : releaseAssets) {
      if (!a.isJsonObject()) {
        throw unexpectedAldaApiResponseError(json);
      }

      JsonObject asset = a.getAsJsonObject();

      JsonPrimitive an = asset.getAsJsonPrimitive("name");
      if (an == null) {
        throw unexpectedAldaApiResponseError(json);
      }
      String assetName = an.getAsString();

      JsonPrimitive au = asset.getAsJsonPrimitive("url");
      if (au == null) {
        throw unexpectedAldaApiResponseError(json);
      }
      String assetUrl = au.getAsString();

      assetNames.add(assetName);

      new Thread() {
        public void run() {
          Path downloadedAsset = Paths.get(assetsDir.toString(), assetName);
          downloadedAssets.add(downloadedAsset);

          try {
            Util.downloadFile(assetUrl, downloadedAsset.toString());
          } catch (Exception e) {
            downloadErrors.add(e);
            downloads.countDown();
            return;
          }

          if (SystemUtils.IS_OS_UNIX) {
            downloadedAsset.toFile().setExecutable(true);
          }

          // We're about to copy this file into a different location; we want to
          // make sure that the temp file gets cleaned up before exiting.
          downloadedAsset.toFile().deleteOnExit();

          downloads.countDown();
        }
      }.start();
    }

    System.out.printf("Downloading %s...\n", String.join(" + ", assetNames));
    try {
      downloads.await();
    } catch (InterruptedException e) {
      throw new UnsuccessfulException("Alda download was interrupted");
    }

    if (!downloadErrors.isEmpty()) {
      // There could be more than one error, but the first one is probably
      // informative enough.
      throw new SystemException("Failed to download Alda", downloadErrors.get(0));
    }

    System.out.println("Stopping any Alda servers you may have running...");
    for (AldaProcess server : existingServers()) {
      AldaServerOptions serverOpts = new AldaServerOptions();
      serverOpts.host = "localhost";
      serverOpts.port = server.port;
      try {
        new AldaServer(serverOpts).down();
      } catch (NoResponseException e) {
        System.out.printf(
          "WARN: Failed to stop Alda server running on port %d!\n",
          server.port
        );
      }
    }

    System.out.printf("Installing %s...\n", String.join(" + ", assetNames));

    // The directory containing the current alda executable
    Path programDir;
    try {
      programDir = Util.getProgramDir();
    } catch (URISyntaxException e) {
      throw new SystemException(
        "Could not determine the path to the current `alda` executable.", e
      );
    }

    // Naïvely, you would think we could simply replace `alda` with the new
    // version that we downloaded. However, this doesn't always work, especially
    // given that we're currently _running_ the old version in this very
    // process.
    //
    // We're doing something safer here, following the advice here:
    // https://stackoverflow.com/a/7198760/2338327
    //
    // (That StackOverflow question is specifically about Windows, but this is a
    // safer method in general, and it works regardless of OS.)
    for (Path asset : downloadedAssets) {
      Path out = Paths.get(programDir.toString(), asset.toFile().getName());

      // First, we rename the existing file, using a convention that Alda v2
      // recognizes. Alda v2 automatically cleans up old, renamed executables
      // like these, so it's fine to leave the old (renamed) executable lying
      // around.
      File oldRenamed = new File(
        String.format(
          "%s.%d.%d.old",
          out.toString(),
          Instant.now().getEpochSecond(),
          new Random().nextInt(10000)
        )
      );
      // NOTE: This is a no-op if there is no existing file, e.g. when upgrading
      // from Alda v1 => v2 and there isn't an alda-player executable yet.
      out.toFile().renameTo(oldRenamed);

      // Now that we've moved the existing executable, we create a new one at
      // the same location and copy the new version into it.
      try {
        Files.copy(asset, out);
      } catch (IOException e) {
        throw new SystemException(
          "Failed to install Alda " + releaseVersion, e
        );
      }
    }

    System.out.println();
    System.out.printf("Successfully updated to Alda %s.\n", releaseVersion);
  }

  public static void listProcesses(AldaServerOptions serverOpts)
    throws SystemException {
    IAldaProcessReader processReader = getProcessReader();
    List<AldaProcess> processes = processReader.getProcesses();

    for (AldaProcess process : processes) {
      if (process.type == "server") {
        if (process.port == -1) {
          System.out.printf("[???] Mysterious server running on unknown " +
                            "port (pid: %d)\n", process.pid);
          System.out.flush();
        } else {
          serverOpts.host = "localhost";
          serverOpts.port = process.port;
          serverOpts.quiet = false;
          AldaServer server = new AldaServer(serverOpts);
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

  private static List<AldaProcess> existingServers() throws SystemException {
    List<AldaProcess> servers = new ArrayList<AldaProcess>();

    for (AldaProcess process : getProcessReader().getProcesses()) {
      if (process.type == "server") {
        servers.add(process);
      }
    }

    return servers;
  }

  public static boolean checkForExistingServer(int port)
    throws SystemException {
    for (AldaProcess server : existingServers()) {
      if (server.port == port) {
        return true;
      }
    }

    return false;
  }
}
