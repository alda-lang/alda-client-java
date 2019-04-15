package alda;

import java.io.File;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParameterException;

import alda.error.AldaException;
import alda.error.ExitCode;
import alda.error.InvalidOptionsException;
import alda.error.SystemException;
import alda.repl.AldaRepl;

public class Main {

  public static class FileConverter implements IStringConverter<File> {
    @Override
    public File convert(String value) {
      return new File(value);
    }
  }

  private static class GlobalOptions {
    @Parameter(names = {"--alda-fingerprint"},
               description = "Used to identify this as an Alda process",
               hidden = true)
    public boolean aldaFingerprint = false;

    @Parameter(names = {"-h", "--help"},
               help = true,
               description = "Print this help text")
    public boolean help = false;

    @Parameter(names = {"-v", "--verbose"},
               description = "Enable verbose output")
    public boolean verbose = false;

    @Parameter(names = {"-q", "--quiet"},
               description = "Disable non-error messages")
    public boolean quiet = false;

    @Parameter(names = {"--no-color"},
               description = "Disable color output.")
    public boolean noColor = false;

    @Parameter(names = {"-H", "--host"},
               description = "The hostname of the Alda server")
    public String host = "localhost";

    @Parameter(names = {"-p", "--port"},
               description = "The port of the Alda server/worker")
    public int port = 27713;

    @Parameter(names = {"-t", "--timeout"},
               description = "The number of seconds to wait for a server to start up or shut down, before giving up.")
    public int timeout = 30;

    @Parameter(names = {"-w", "--workers"},
               description = "The number of worker processes to start")
    public int numberOfWorkers = 2;
  }

  private static class AldaCommand {
      @Parameter(names = {"--help", "-h"},
                 help = true,
                 hidden = true,
                 description = "Print this help text")
      public boolean help = false;
  }

  @Parameters(commandDescription = "Start an Alda server in the foreground.",
              hidden = true)
  private static class CommandServer extends AldaCommand {}

  @Parameters(commandDescription = "Start an Alda worker in the foreground.",
              hidden = true)
  private static class CommandWorker extends AldaCommand {}

  @Parameters(commandDescription = "Start an interactive Alda REPL session.")
  private static class CommandRepl extends AldaCommand {}

  @Parameters(commandDescription = "Display this help text")
  private static class CommandHelp extends AldaCommand {}

  @Parameters(commandDescription = "Download and install the latest release of Alda")
  private static class CommandUpdate extends AldaCommand {}

  @Parameters(commandDescription = "Start the Alda server")
  private static class CommandStartServer extends AldaCommand {}

  @Parameters(commandDescription = "Stop the Alda server")
  private static class CommandStopServer extends AldaCommand {}

  @Parameters(commandDescription = "Restart the Alda server")
  private static class CommandRestartServer extends AldaCommand {}

  @Parameters(commandDescription = "List running Alda servers/workers")
  private static class CommandList extends AldaCommand {}

  @Parameters(commandDescription = "Display whether the server is up")
  private static class CommandStatus extends AldaCommand {}

  @Parameters(commandDescription = "Display the version of the Alda client and server")
  private static class CommandVersion extends AldaCommand {}

  @Parameters(commandDescription = "Evaluate and play Alda code")
  private static class CommandPlay extends AldaCommand {
    @Parameter(names = {"-f", "--file"},
               description = "Read Alda code from a file",
               converter = FileConverter.class)
    public File file;

    @Parameter(names = {"-c", "--code"},
               description = "Supply Alda code as a string")
    public String code;

    @Parameter(names = {"-i", "--history"},
               description = "Alda code that can be referenced but will not be played")
    public String history = "";

    @Parameter(names = {"-I", "--history-file"},
               description = "A file containing Alda code that can be referenced but will not be played",
               converter = FileConverter.class)
    public File historyFile;

    @Parameter(names = {"-F", "--from"},
               description = "A time marking or marker from which to start " +
                             "playback")
    public String from;

    @Parameter(names = {"-T", "--to"},
               description = "A time marking or marker at which to end playback")
    public String to;
  }

  @Parameters(commandDescription = "Stop playback")
  private static class CommandStop extends AldaCommand {}

  @Parameters(commandDescription = "Display the result of parsing Alda code")
  private static class CommandParse extends AldaCommand {
    @Parameter(names = {"-f", "--file"},
               description = "Read Alda code from a file",
               converter = FileConverter.class)
    public File file;

    @Parameter(names = {"-c", "--code"},
               description = "Supply Alda code as a string")
    public String code;

    @Parameter(names = {"-o", "--output"},
               description = "Return the output as \"data\" or \"events\"")
    public String outputType = "data";
  }

  @Parameters(commandDescription = "Display a list of available instruments")
  private static class CommandInstruments extends AldaCommand {}

  public static void handleCommandSpecificHelp(JCommander jc, String name, AldaCommand c) {
    if(c.help) {
      jc.usage(name);
      ExitCode.SUCCESS.exit();
    }
  }

  @Parameters(
    commandDescription =
      "Evaluate Alda code and export the score to another format"
  )
  private static class CommandExport extends AldaCommand {
    @Parameter(names = {"-f", "--file"},
               description = "Read Alda code from a file",
               converter = FileConverter.class)
    public File file;

    @Parameter(names = {"-c", "--code"},
               description = "Supply Alda code as a string")
    public String code;

    @Parameter(names = {"-F", "--output-format"},
               description = "The output format (options: midi)")
    public String outputFormat;

    @Parameter(names = {"-o", "--output"},
               description = "The output filename")
    public String outputFilename;
  }

  public static void main(String[] argv) {
    GlobalOptions globalOpts = new GlobalOptions();

    CommandHelp          help          = new CommandHelp();
    CommandUpdate        update        = new CommandUpdate();
    CommandServer        serverCmd     = new CommandServer();
    CommandWorker        workerCmd     = new CommandWorker();
    CommandRepl          repl          = new CommandRepl();
    CommandStartServer   startServer   = new CommandStartServer();
    CommandStopServer    stopServer    = new CommandStopServer();
    CommandRestartServer restartServer = new CommandRestartServer();
    CommandList          list          = new CommandList();
    CommandStatus        status        = new CommandStatus();
    CommandVersion       version       = new CommandVersion();
    CommandPlay          play          = new CommandPlay();
    CommandStop          stop          = new CommandStop();
    CommandParse         parse         = new CommandParse();
    CommandInstruments   instruments   = new CommandInstruments();
    CommandExport        export        = new CommandExport();

    JCommander jc = new JCommander(globalOpts);
    jc.setProgramName("alda");

    jc.addCommand("help", help);

    jc.addCommand("update", update);

    jc.addCommand("server", serverCmd);
    jc.addCommand("worker", workerCmd);
    jc.addCommand("repl", repl);

    jc.addCommand("up", startServer, "start-server", "init");
    jc.addCommand("down", stopServer, "stop-server");
    jc.addCommand("downup", restartServer, "restart-server");

    jc.addCommand("list", list);
    jc.addCommand("status", status);
    jc.addCommand("version", version);

    jc.addCommand("play", play);
    jc.addCommand("stop", stop, "stop-playback");
    jc.addCommand("parse", parse);
    jc.addCommand("instruments", instruments);
    jc.addCommand("export", export);

    try {
      jc.parse(argv);
    } catch (ParameterException e) {
      System.out.println(e.getMessage());
      System.out.println();
      System.out.println("For usage instructions, see --help.");
      ExitCode.USER_ERROR.exit();
    }

    AldaServerOptions serverOpts = new AldaServerOptions();
    serverOpts.host    = globalOpts.host;
    serverOpts.port    = globalOpts.port;
    serverOpts.timeout = globalOpts.timeout;
    serverOpts.verbose = globalOpts.verbose;
    serverOpts.quiet   = globalOpts.quiet;
    serverOpts.noColor = globalOpts.noColor;

    AldaServer server = new AldaServer(serverOpts);

    try {
      if (globalOpts.help) {
        jc.usage();
        return;
      }

      String command = jc.getParsedCommand();
      command = command == null ? "help" : command;

      // used for play, parse and export commands
      String input;

      // used for up and downup commands
      boolean success;

      switch (command) {
        case "help":
          jc.usage();
          break;

        case "update":
          handleCommandSpecificHelp(jc, "update", update);
          AldaClient.updateAlda();
          break;

        case "server":
          handleCommandSpecificHelp(jc, "server", serverCmd);
          server.upFg(globalOpts.numberOfWorkers);
          break;

        case "worker":
          handleCommandSpecificHelp(jc, "worker", workerCmd);
          AldaWorker worker = new AldaWorker(globalOpts.port,
                                             globalOpts.verbose);

          worker.upFg();
          break;

        case "repl":
          handleCommandSpecificHelp(jc, "repl", repl);
          AldaRepl javaRepl = new AldaRepl(server, globalOpts.verbose);
          javaRepl.run();
          break;

        case "up":
        case "start-server":
        case "init":
          handleCommandSpecificHelp(jc, "up", startServer);
          server.upBg(globalOpts.numberOfWorkers);
          break;

        case "down":
        case "stop-server":
          handleCommandSpecificHelp(jc, "down", stopServer);
          server.down();
          break;

        case "downup":
        case "restart-server":
          handleCommandSpecificHelp(jc, "restart-server", restartServer);
          server.downUp(globalOpts.numberOfWorkers);
          break;

        case "list":
          handleCommandSpecificHelp(jc, "list", list);
          AldaClient.listProcesses(serverOpts);
          break;

        case "status":
          handleCommandSpecificHelp(jc, "status", status);
          server.status();
          break;

        case "version":
          handleCommandSpecificHelp(jc, "version", version);
          System.out.println("Client version: " + AldaClient.version());
          System.out.print("Server version: ");
          server.version();
          break;

        case "play":
          handleCommandSpecificHelp(jc, "play", play);

          input = findInput(play.file, play.code);

          if (play.historyFile != null) {
            if (!play.history.isEmpty())
              throw new InvalidOptionsException(
                "--history and --history-file options cannot be used together."
              );

            play.history = Util.readFile(play.historyFile);
          }

          server.play(input, play.history, play.from, play.to);
          break;

        case "stop":
        case "stop-playback":
            handleCommandSpecificHelp(jc, "stop", stop);
            server.stop();
            break;

        case "parse":
          handleCommandSpecificHelp(jc, "parse", parse);

          input = findInput(parse.file, parse.code);

          if (!(parse.outputType.equals("data") ||
                parse.outputType.equals("events")))
            throw new InvalidOptionsException(
              "Invalid --output type. Valid output types are: data, events"
            );

          server.parse(input, parse.outputType);
          break;

        case "instruments":
          handleCommandSpecificHelp(jc, "instruments", instruments);
          server.displayInstruments();
          break;

        case "export":
          handleCommandSpecificHelp(jc, "export", export);

          input = findInput(export.file, export.code);

          // We currently only support exporting to MIDI. For now, we can allow
          // the --output-format option to be omitted, and assume that the user
          // means MIDI.
          //
          // In the future, if/when we add other output formats, we should try
          // to infer the output format from the file extension.
          if (export.outputFormat == null)
            export.outputFormat = "midi";

          if (!export.outputFormat.equals("midi"))
            throw new InvalidOptionsException(
              "Invalid --output-format. Valid output formats are: midi"
            );

          if (export.outputFilename == null)
            throw new InvalidOptionsException(
              "You must specify an --output filename."
            );

          server.export(input, export.outputFormat, export.outputFilename);
          break;
      }
    } catch (AldaException e) {
      server.error(e.getMessage());
      if (globalOpts.verbose) {
        System.out.println();
        e.printStackTrace();
      }
      e.getExitCode().exit();
    }
    ExitCode.SUCCESS.exit();
  }

  private static boolean receivingInputFromStdin() {
    return System.console() == null;
  }

  private static String findInput(File file, String code)
    throws InvalidOptionsException, SystemException {
    if (file != null && code != null) {
      throw new InvalidOptionsException(
        "You must supply either a --file or --code argument (not both)."
      );
    }

    if (file != null) return Util.readFile(file);
    if (code != null) return code;
    if (receivingInputFromStdin()) return Util.getStdIn();

    throw new InvalidOptionsException(
      "Please provide some Alda code in the form of a string, file, or STDIN."
    );
  }
}
