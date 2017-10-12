package alda;

import alda.error.NoResponseException;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class AldaProcess {
  public boolean verbose = false;
  public boolean quiet = false;
  public String host;
  public int pid;
  public int port;
  public String type;
  public int timeout;
}
