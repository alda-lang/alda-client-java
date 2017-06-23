package alda.error;

public class AlreadyUpException extends AldaException {

  public AlreadyUpException(String msg) {
    super(msg);
  }

  @Override
  public ExitCode getExitCode() { return ExitCode.USER_ERROR; }

}
