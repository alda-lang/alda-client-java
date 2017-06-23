package alda.error;

public abstract class AldaException extends Exception {
  public AldaException(String msg) {
    super(msg);
  }

  public AldaException(String msg, Throwable e) {
    super(msg, e);
  }

  public abstract ExitCode getExitCode();
}
