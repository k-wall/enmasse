package io.enmasse.systemtest.marathon;

import io.enmasse.systemtest.AddressSpace;
import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.Destination;
import io.enmasse.systemtest.Logging;
import io.enmasse.systemtest.web.ConsoleWebPage;
import io.enmasse.systemtest.web.ISeleniumProvider;
import io.enmasse.systemtest.web.SeleniumProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.openqa.selenium.WebDriver;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.stream.IntStream;

public abstract class WebConsoleTest extends MarathonTestBase implements ISeleniumProvider {

    SeleniumProvider selenium = new SeleniumProvider();
    AddressSpace addressSpace;

    @Rule
    public TestWatcher watchman = new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
            selenium.onFailed(e, description);
        }
    };
    ConsoleWebPage consoleWebPage;

    @Before
    public void setUpWebConsoleTests() throws Exception {
        Logging.log.info("testCreateDeleteUsersLong start");
        addressSpace = new AddressSpace("brokered-marathon-web-console",
                AddressSpaceType.BROKERED);
        createAddressSpace(addressSpace, "standard");
        Logging.log.info("Address space '{}'created", addressSpace);

        username = "test";
        password = "test";
        createUser(addressSpace, username, username);

        selenium.setupDriver(environment, openShift, buildDriver());
        consoleWebPage = new ConsoleWebPage(selenium, getConsoleRoute(addressSpace), addressApiClient, addressSpace);
    }

    @After
    public void tearDownDrivers() throws Exception {
        selenium.tearDownDrivers();
        deleteAddressSpace(addressSpace);
    }

    public void doTestCreateDeleteAddressesViaAgentLong() throws Exception {
        int addressCount = 5;
        ArrayList<Destination> addresses = generateQueueTopicList("via-web", IntStream.range(0, addressCount));

        runTestInLoop(30, () -> {
            consoleWebPage.createAddressesWebConsole(addresses.toArray(new Destination[0]));
            consoleWebPage.deleteAddressesWebConsole(addresses.toArray(new Destination[0]));
            Thread.sleep(5000);
        });
        Logging.log.info("testCreateDeleteAddressesViaAgentLong finished");
    }

    @Override
    public WebDriver buildDriver() {
        throw new NotImplementedException();
    }
}
