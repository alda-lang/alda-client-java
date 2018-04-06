package alda.testutils;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;

public class AldaJunitRunListener extends RunListener {

  @Override
  public void testRunStarted(Description description) throws Exception {
    System.out.println(AldaJunitRunListener.class.getName() + ": bringing up test environment..");
    if(TestEnvironment.getStatus() == TestEnvironmentStatus.STOPPED){
      TestEnvironment.setUp();
    }
  }

  // Called when all tests have finished
  @Override
  public void testRunFinished(Result result) throws Exception {
    System.out.println(AldaJunitRunListener.class.getName() + ": tearing down test environment..");
    TestEnvironment.tearDown();
  }
}
