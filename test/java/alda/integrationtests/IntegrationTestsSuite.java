package alda.integrationtests;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import alda.integrationtests.commands.ReplCommandsTestSuite;
import alda.testutils.TestEnvironment;
import alda.testutils.TestEnvironmentStatus;

@RunWith(Suite.class)
@SuiteClasses({
  AldaClientTest.class,
  ReplCommandsTestSuite.class
})
public class IntegrationTestsSuite {
  
  @BeforeClass public static void setUp() {
    System.err.println("Preparing test environment...");
    if(TestEnvironment.getStatus() == TestEnvironmentStatus.STOPPED){
      try {
        TestEnvironment.setUp();
      } catch(Exception e) {
        System.err.println(e.getMessage());
        throw new RuntimeException(e);
      }
    }
  }
  
  @AfterClass public static void tearDown() {
    System.out.println("Shutting down test environment...");
    try {
      TestEnvironment.tearDown();
    } catch(Exception e) {
      System.err.println(e.getMessage());
      throw new RuntimeException(e);
    }
  }
  
}
