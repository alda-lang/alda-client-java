package alda;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

import jline.console.ConsoleReader;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.ISeq;
import clojure.lang.Symbol;
import clojure.lang.ArraySeq;
import com.google.gson.*;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;

public final class Util {

  public static Object[] concat(Object[] a, Object[] b) {
    int aLen = a.length;
    int bLen = b.length;
    Object[] c = new Object[aLen+bLen];
    System.arraycopy(a, 0, c, 0, aLen);
    System.arraycopy(b, 0, c, aLen, bLen);
    return c;
  }

  public static Object[] conj(Object[] a, Object b) {
    return concat(a, new Object[]{b});
  }

  // Given an array of choices like {"yes", "no", "quit"}, offers them to the
  // user as choices in this format: (y)es, (n)o, (q)uit
  //
  // Returns the selected string as soon as the user presses the corresponding
  // key.
  public static String promptWithChoices(ConsoleReader rdr,
                                         List<String> choices)
  throws IOException {
    char[] allowedChars = new char[choices.size()];
    Map<Character, String> m = new LinkedHashMap<Character, String>();

    for (int i = 0; i < choices.size(); i++) {
      String choice = choices.get(i);
      char c = choice.charAt(0);
      allowedChars[i] = c;
      m.put(c, choice);
    }

    String choiceString = choices.stream()
                                 .map(str -> "(" + str.charAt(0) + ")"
                                             + str.substring(1))
                                 .collect(Collectors.joining(", "));

    System.out.println(choiceString);

    // Read characters until the user enters one that is an option.
    char c = (char)rdr.readCharacter(allowedChars);
    return m.get(c);
  }

  /**
   * Small wrapper to make prompts easier to output.
   * WARNING: This exits the whole program if any read error is given (and is unchecked).
   * @param r The console reader to input on
   * @param choices A varargs of choices to give
   * @param prompt The prompt to display above the choice.
   * @return The choice selected by the user.
   */
  public static String promptWithChoices(ConsoleReader r, String prompt, String... choices) {
    try {
      System.out.println(prompt);
      return promptWithChoices(r, Arrays.asList(choices));
    } catch (IOException e) {
      System.err.println("There was an error reading input!");
      e.printStackTrace();
      System.exit(1);
    }
    return null;
  }

  public static boolean promptForConfirmation(String prompt) {
    Console console = System.console();
    if (System.console() != null) {
      Boolean confirm = null;
      while (confirm == null) {
        String response = console.readLine(prompt + " (y/n) ");
        if (response.toLowerCase().startsWith("y")) {
          confirm = true;
        } else if (response.toLowerCase().startsWith("n")) {
          confirm = false;
        }
      }
      return confirm.booleanValue();
    } else {
      System.out.println(prompt + "\n");
      System.out.println("Unable to get a response because you are " +
                         "redirecting input.\nI'm just gonna assume the " +
                         "answer is no.\n\n" +
                         "To auto-respond yes, use the -y/--yes option.");
      return false;
    }
  }

  public static String inputType(File file, String code)
    throws InvalidOptionsException {
    if (file == null && code == null) {
      // check to see if we're receiving input from STDIN
      if (System.console() == null) {
        return "stdin";
      } else {
        // if not, input type is the existing score in its entirety
        return "score";
      }
    }

    if (file != null && code != null) {
      throw new InvalidOptionsException("You must supply either a --file or " +
                                        "--code argument (not both).");
    }

    if (file != null) {
      return "file";
    } else {
      return "code";
    }
  }

  public static String getStdIn() {
    String fromStdIn = "";
    Scanner scanner = new Scanner(System.in);
    while (scanner.hasNextLine()) {
      fromStdIn += scanner.nextLine() + "\n";
    }
    return fromStdIn;
  }

  public static String getProgramPath() throws URISyntaxException {
    return Main.class.getProtectionDomain().getCodeSource().getLocation()
               .toURI().getPath();
  }

  public static String makeApiCall(String apiRequest) throws IOException {
      URL url = new URL(apiRequest);
      HttpURLConnection conn =
        (HttpURLConnection) url.openConnection();

      if (conn.getResponseCode() != 200) {
        throw new IOException(conn.getResponseMessage());
      }

      // Buffer the result into a string
      BufferedReader rd = new BufferedReader(
        new InputStreamReader(conn.getInputStream()));
      StringBuilder sb = new StringBuilder();
      String line;
      line = rd.readLine();
      while (line != null) {
        sb.append(line);
        line = rd.readLine();
      }
      rd.close();
      conn.disconnect();
      return sb.toString();
  }

  public static void downloadFile(String url, String path) throws IOException {
    BufferedInputStream in = null;
    FileOutputStream fout = null;
    try {
      in = new BufferedInputStream(new URL(url).openStream());
      fout = new FileOutputStream(path);

      final byte data[] = new byte[1024];
      int count;
      while ((count = in.read(data, 0, 1024)) != -1) {
        fout.write(data, 0, count);
      }
    } finally {
      // Close file IO's
      if (in != null) in.close();
      if (fout != null) fout.close();
    }
  }

  // Helper method to move files around, handling exceptions
  public static boolean moveFile(File oldFile, File newFile) {
    boolean success = false;
    Exception toSave = null;
    try {
      success = oldFile.renameTo(newFile);
    } catch (SecurityException e) {
      success = false;
      toSave = e;
    } finally {
      if (!success) {
        System.err.println("There was an error copying '" + oldFile + "' to '" + newFile +  "'");
        if (toSave != null)
          toSave.printStackTrace();
        return false;
      }
      return true;
    }
  }

  public static String readFile(File file) throws IOException {
    return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
  }

  public static String readResourceFile(String path) {
    StringBuilder out = new StringBuilder();
    BufferedReader reader = null;
    try {
      InputStream in = Util.class.getClassLoader().getResourceAsStream(path);
      reader = new BufferedReader(new InputStreamReader(in));
      String line;
      while ((line = reader.readLine()) != null) {
        out.append(line);
      }
    } catch(IOException e) {
      System.err.println("There was an error reading a file!");
      e.printStackTrace();
    } finally {
      try {
        reader.close();
      } catch (Exception e) {
        // Theres nothing we can do...
        System.err.println("There was a critical error!");
        e.printStackTrace();
        return null;
      }
    }
    return out.toString();
  }

  public static void forkProgram(Object... args)
    throws URISyntaxException, IOException {
    String programPath = getProgramPath();

    Object[] program;
    if (programPath.endsWith(".jar")) {
      program = new Object[]{"java", "-jar", programPath};
    } else {
      program = new Object[]{programPath};
    }

    Object[] objectArray = concat(program, args);
    String[] execArgs = Arrays.copyOf(objectArray, objectArray.length, String[].class);

    Process p = Runtime.getRuntime().exec(execArgs);
    p.getInputStream().close();
    p.getOutputStream().close();
    p.getErrorStream().close();
  }

  public static void runProgramInFg(String... args)
  throws IOException, InterruptedException {
    new ProcessBuilder(args).inheritIO().start().waitFor();
  }

  public static void callClojureFn(String fn, Object... args) {
    Symbol var = (Symbol)Clojure.read(fn);
    IFn require = Clojure.var("clojure.core", "require");
    require.invoke(Symbol.create(var.getNamespace()));
    ISeq argsSeq = ArraySeq.create(args);
    Clojure.var(var.getNamespace(), var.getName()).applyTo(argsSeq);
  }
}
