package alda.error;

public class ParseError extends AldaException {

  public ParseError(String msg) {
    super(msg);
  }

  @Override
  public ExitCode getExitCode() { return ExitCode.USER_ERROR; }

}
