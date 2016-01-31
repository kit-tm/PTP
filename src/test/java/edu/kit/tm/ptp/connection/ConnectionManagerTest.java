package edu.kit.tm.ptp.connection;

import static org.junit.Assert.assertEquals;

import edu.kit.tm.ptp.Configuration;
import edu.kit.tm.ptp.Identifier;
import edu.kit.tm.ptp.PTP;
import edu.kit.tm.ptp.SendListener;
import edu.kit.tm.ptp.SendReceiveListener;
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
    ConnectionManager manager = new ConnectionManager(Constants.localhost, 1000, 1001);
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
    SendReceiveListener listener = new SendReceiveListener();
    ptp = new PTP();
    ptp.createHiddenService();

    Configuration config = ptp.getConfiguration();

    Serializer serializer = new Serializer();

    ConnectionManager manager = new ConnectionManager(Constants.localhost,
        config.getTorSOCKSProxyPort(), config.getHiddenServicePort());
    manager.setSendListener(listener);
    manager.setLocalIdentifier(new Identifier("bla.onion"));
    manager.setSerializer(serializer);
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

}
