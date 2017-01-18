package alda;

import alda.integrationtests.AldaClientTest;
import alda.testutils.TestEnvironment;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        AldaClientTest.class
})
public class IntegrationTestsSuite {

    @BeforeClass
    public static void setUp() throws Exception{
        TestEnvironment.setUp();
    }

    @AfterClass
    public static void tearDown() throws Exception{
        TestEnvironment.tearDown();
    }
}
