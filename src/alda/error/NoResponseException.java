package alda.error;

public class NoResponseException extends AldaException {

  public NoResponseException(String msg) {
    super(msg);
  }

  @Override
  public ExitCode getExitCode() { return ExitCode.NETWORK_ERROR; }

}
