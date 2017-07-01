package alda.error;

public enum ExitCode {
  SUCCESS(0),
  UNSPECIFIED_ERROR(1),
  RUNTIME_ERROR(2),
  USER_ERROR(3),
  SYSTEM_ERROR(4),
  NETWORK_ERROR(5);

  private int value;
  ExitCode(int value) { this.value = value; }
  public int getValue() { return value; }

  public void exit() { System.exit(value); }

}


