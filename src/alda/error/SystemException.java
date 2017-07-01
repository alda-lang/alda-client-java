package alda.error;

public class SystemException extends AldaException {
  public SystemException(String msg) {
    super(msg);
  }

  public SystemException(String msg, Throwable e) {
    super(msg, e);
  }

  @Override
  public ExitCode getExitCode() { return ExitCode.SYSTEM_ERROR; }

}
