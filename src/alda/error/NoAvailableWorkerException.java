package alda.error;

public class NoAvailableWorkerException extends AldaException {

  public NoAvailableWorkerException(String msg) {
    super(msg);
  }

  @Override
  public ExitCode getExitCode() { return ExitCode.RUNTIME_ERROR; }

}
