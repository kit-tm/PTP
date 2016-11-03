package edu.kit.tm.ptp;

import org.junit.After;
import org.junit.Test;

import java.io.IOException;

import edu.kit.tm.ptp.auth.DummyAuthenticatorFactory;
import edu.kit.tm.ptp.connection.ConnectionManager;
import edu.kit.tm.ptp.serialization.ByteArrayMessage;
import edu.kit.tm.ptp.serialization.Serializer;
import edu.kit.tm.ptp.utility.Constants;
import edu.kit.tm.ptp.utility.TestConstants;
import edu.kit.tm.ptp.utility.TestHelper;

import static org.junit.Assert.assertEquals;

/**
 * Test class for IsAliveManager.
 */

public class IsAliveManagerTest {
  private PTP ptp = null;

  public void setUp() throws IOException {
    ptp = new PTP(true);
  }

  @After
  public void tearDown() {
    ptp.exit();
  }

  @Test
  public void testIsAlive() throws IOException {
    ConfigurationFileReader configReader = new ConfigurationFileReader(Constants.configfile);
    Configuration config =  configReader.readFromFile();

    int isAliveTimeout = 10 * 1000;
    int isAliveSendTimeout = 5 * 1000;

    config.setIsAliveValues(isAliveTimeout, isAliveSendTimeout);

    ptp = new PTP(true, config);
    ptp.authFactory = new DummyAuthenticatorFactory();
    ptp.init();
    ptp.reuseHiddenService();

    Serializer serializer = new Serializer();
    serializer.registerClass(byte[].class);
    serializer.registerClass(ByteArrayMessage.class);

    SendReceiveListener listener = new SendReceiveListener();

    ConnectionManager manager =
        new ConnectionManager(config.getHiddenServicePort(), listener, listener, null,
            new DummyAuthenticatorFactory());
    manager.updateSOCKSProxy(Constants.localhost, config.getTorSOCKSProxyPort());
    manager.setLocalIdentifier(new Identifier("aaaaaaaaaaaaaaaa.onion"));
    manager.start();

    Identifier identifier = ptp.getIdentifier();

    byte[] data = new byte[] {0x0, 0x1, 0x2, 0x3};
    ByteArrayMessage message = new ByteArrayMessage(data);
    long id = manager.send(serializer.serialize(message), identifier,
        TestConstants.hiddenServiceSetupTimeout);

    TestHelper.wait(listener.sent, 1, TestConstants.hiddenServiceSetupTimeout);

    assertEquals(1, listener.sent.get());

    TestHelper.wait(listener.received, 1, isAliveSendTimeout + TestConstants.listenerTimeout);

    assertEquals(1, listener.received.get());
  }
}
