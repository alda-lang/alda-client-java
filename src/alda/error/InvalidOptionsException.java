package alda.error;

public class InvalidOptionsException extends AldaException {

  public InvalidOptionsException(String msg) {
    super(msg);
  }

  @Override
  public ExitCode getExitCode() { return ExitCode.USER_ERROR; }

}
