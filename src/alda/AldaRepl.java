
package alda;

import java.io.IOException;

import jline.console.ConsoleReader;
import jline.console.completer.Completer;

import org.fusesource.jansi.AnsiConsole;
import org.fusesource.jansi.Ansi.Color;
import static org.fusesource.jansi.Ansi.*;
import static org.fusesource.jansi.Ansi.Color.*;

public class AldaRepl {
  public static final String ASCII_ART  =
    " █████╗ ██╗     ██████╗  █████╗\n" +
    "██╔══██╗██║     ██╔══██╗██╔══██╗\n" +
    "███████║██║     ██║  ██║███████║\n" +
    "██╔══██║██║     ██║  ██║██╔══██║\n" +
    "██║  ██║███████╗██████╔╝██║  ██║\n" +
    "╚═╝  ╚═╝╚══════╝╚═════╝ ╚═╝  ╚═╝\n";

  public static final int ASCII_WIDTH = ASCII_ART.substring(0, ASCII_ART.indexOf('\n')).length();
  public static final String HELP_TEXT = "Type :help for a list of available commands.";
  public static final String PROMPT = "> ";

  private AldaServer server;
  private ConsoleReader r;
  private boolean verbose;

  private StringBuffer history;

  public AldaRepl(AldaServer server, boolean verbose) {
    this.server = server;
    this.verbose = verbose;
    history = new StringBuffer();
    try {
      r = new ConsoleReader();
    } catch (IOException e) {
      System.err.println("An error was detected when we tried to read a line.");
      e.printStackTrace();
      System.exit(1);
    }

    AnsiConsole.systemInstall();
  }

  /**
   * Centers and colors text
   * @param totalLen the total length to center to
   * @param toFormat the string to format
   * @param color the ANSI code to color. null will result in no color
   */
  private String centerText(int totalLen, String toFormat, Color color) {
    int offset = totalLen / 2 - toFormat.length() / 2;
    String out = "";
    if (offset > 0) {
      // Print out spaces to center the version string
      out = out + String.format("%1$"+offset+"s", " ");
    }
    out = out + toFormat;
    if (color != null) {
      out = ansi().fg(color).a(out).reset().toString();
    }
    return out;
  }

  public void run() {
    System.out.println(ansi().fg(BLUE).a(ASCII_ART).reset());
    System.out.println(centerText(ASCII_WIDTH, AldaClient.version(), CYAN));
    System.out.println(centerText(ASCII_WIDTH, "repl session", CYAN));

    System.out.println("\n" + ansi().fg(WHITE).bold().a(HELP_TEXT).reset() + "\n");

    while (true) {
      String input = "";
      try {
        // TODO add dynamic prompts based on the instrument
        input = r.readLine(PROMPT);
      } catch (IOException e) {
        System.err.println("An error was detected when we tried to read a line.");
        e.printStackTrace();
        System.exit(1);
      }

      // TODO check for :keywords and act on them

      try {
        // TODO get all playing errors to come up here via exceptions.
        server.play(input, history.toString(), null, null);
      } catch (Throwable e) {
        server.error(e.getMessage());
        if (verbose) {
          System.out.println();
          e.printStackTrace();
        }
        System.exit(1);
      }

      // Add to history
      history.append(input);
      history.append("\n");
    }
  }
}
