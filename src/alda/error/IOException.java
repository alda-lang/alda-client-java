package alda.error;

public class IOException extends AldaException {
  public IOException(String msg) {
    super(msg);
  }

  public IOException(String msg, Throwable e) {
    super(msg, e);
  }

  @Override
  public ExitCode getExitCode() { return ExitCode.SYSTEM_ERROR; }

}
