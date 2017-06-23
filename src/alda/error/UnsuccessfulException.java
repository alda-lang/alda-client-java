package alda.error;

public class UnsuccessfulException extends AldaException {

  public UnsuccessfulException(String msg) {
    super(msg);
  }

  @Override
  public ExitCode getExitCode() { return ExitCode.UNSPECIFIED_ERROR; }

}
