package edu.kit.tm.ptp;

import static org.junit.Assert.assertEquals;

import edu.kit.tm.ptp.Configuration;
import edu.kit.tm.ptp.Identifier;
import edu.kit.tm.ptp.PTP;
import edu.kit.tm.ptp.SendListener;
import edu.kit.tm.ptp.auth.DummyAuthenticatorFactory;
import edu.kit.tm.ptp.connection.ConnectionManager;
import edu.kit.tm.ptp.serialization.ByteArrayMessage;
import edu.kit.tm.ptp.serialization.Serializer;
import edu.kit.tm.ptp.utility.Constants;
import edu.kit.tm.ptp.utility.TestConstants;
import edu.kit.tm.ptp.utility.TestHelper;

import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ConnectionManagerTest {
  private ConnectionManager manager;
  private PTP ptp = null;

  @After
  public void tearDown() throws IOException {
    if (manager != null) {
      manager.stop();
    }

    if (ptp != null) {
      ptp.exit();
    }
  }

  @Test
  public void testStartBindServer() throws IOException {
    SendReceiveListener listener = new SendReceiveListener();

    manager = new ConnectionManager(1000, listener, listener, null);// Dummy port
    manager.start();
    int port = manager.startBindServer(Constants.anyport);

    Socket socket = new Socket();
    socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), port),
        TestConstants.socketConnectTimeout);

    assertEquals(true, socket.isConnected());
    socket.close();
  }

  @Test
  public void testSend() throws IOException {
    ptp = new PTP(true);
    ptp.authFactory = new DummyAuthenticatorFactory();
    ptp.init();
    ptp.reuseHiddenService();

    Configuration config = ptp.getConfiguration();
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

    byte[] data = new byte[] {0x0, 0x1, 0x2, 0x3};
    ByteArrayMessage message = new ByteArrayMessage(data);
    long id = manager.send(serializer.serialize(message), ptp.getIdentifier(),
        TestConstants.hiddenServiceSetupTimeout);

    TestHelper.wait(listener.sent, 1, TestConstants.hiddenServiceSetupTimeout);

    assertEquals(1, listener.sent.get());

    assertEquals(id, listener.id);
    assertEquals(SendListener.State.SUCCESS, listener.state);
    assertEquals(ptp.getIdentifier(), listener.destination);
  }

  @Test(expected = IllegalArgumentException.class)
  public void invalidLocalIdentifier() {
    SendReceiveListener listener = new SendReceiveListener();

    manager = new ConnectionManager(1000, listener, listener, null);// Dummy port
    manager.setLocalIdentifier(new Identifier("xyz.onion"));
  }

}
