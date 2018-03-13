package alda.integrationtests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import alda.integrationtests.commands.ReplCommandsTestSuite;

@RunWith(Suite.class)
@SuiteClasses({
  AldaClientTest.class,
  ReplCommandsTestSuite.class
})
public class IntegrationTestsSuite {
  
}
