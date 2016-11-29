package edu.kit.tm.ptp.channels;

import static org.junit.Assert.assertEquals;

import edu.kit.tm.ptp.PTP;
import edu.kit.tm.ptp.utility.Constants;
import edu.kit.tm.ptp.utility.Listener;
import edu.kit.tm.ptp.utility.TestConstants;
import edu.kit.tm.ptp.utility.TestHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Test class for the ChannelManager.
 * 
 * @author Timon Hackenjos
 *
 */
public class ChannelManagerTest {
  private ServerSocketChannel server = null;
  private ChannelManager channelManager;
  private Listener listener;
  private PTP ptp;

  @Before
  public void setUp() throws IOException {
    server = ServerSocketChannel.open();
    server.socket()
        .bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), Constants.anyport));
    server.configureBlocking(false);

    listener = new Listener();
    channelManager = new ChannelManager(listener);
    
    ptp = new PTP(true);
  }

  @After
  public void tearDown() throws IOException {
    if (server != null) {
      server.close();
    }

    if (channelManager != null) {
      channelManager.stop();
    }

    if (ptp != null) {
      ptp.exit();
    }
  }

  @Test
  public void testaddServerSocket() throws IOException {
    channelManager.start();
    channelManager.addServerSocket(server);

    Socket client = new Socket();
    client.connect(
        new InetSocketAddress(InetAddress.getLoopbackAddress(), server.socket().getLocalPort()),
        TestConstants.socketConnectTimeout);

    TestHelper.wait(listener.conOpen, 1, TestConstants.socketConnectTimeout);
    client.close();

    assertEquals(1, listener.conOpen.get());
    assertEquals(0, listener.conClosed.get());
    assertEquals(0, listener.read.get());
    assertEquals(0, listener.write.get());
  }

  @Test
  public void testConnectThroughSOCKS() throws IOException {
    channelManager.start();

    ptp.init();
    ptp.reuseHiddenService();

    // wait for hidden service to become available
    TestHelper.sleep(TestConstants.hiddenServiceSetupTimeout);

    SocketChannel client = SocketChannel.open();
    client.configureBlocking(false);
    client.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(),
        ptp.getConfiguration().getTorSOCKSProxyPort()));
    channelManager.connect(client);

    TestHelper.wait(listener.conOpen, 1, TestConstants.socketConnectTimeout);

    assertEquals(1, listener.conOpen.get());
    assertEquals(0, listener.conClosed.get());
    assertEquals(0, listener.read.get());
    assertEquals(0, listener.write.get());

    SOCKSChannel socksChannel = new SOCKSChannel(listener.passedChannel, channelManager);
    channelManager.addChannel(socksChannel);
    socksChannel.connectThroughSOCKS(ptp.getIdentifier().toString(),
        ptp.getConfiguration().getHiddenServicePort());

    // hidden service should be immediately available
    TestHelper.wait(listener.conOpen, 2, TestConstants.socketConnectTimeout);

    assertEquals(2, listener.conOpen.get());
    assertEquals(0, listener.write.get());
    assertEquals(socksChannel, listener.passedChannel);
    assertEquals(0, listener.conClosed.get());
    
    // SOCKSChannel disabled reading, enable it
    channelManager.registerRead(socksChannel, true);

    ptp.exit();
    ptp = null;

    TestHelper.wait(listener.conClosed, 1, TestConstants.listenerTimeout);

    assertEquals(1, listener.conClosed.get());
    assertEquals(socksChannel, listener.passedChannel);

  }

  @Test
  public void testConnectRemoveChannel() throws IOException {
    channelManager.start();

    SocketChannel client = SocketChannel.open();
    final MessageChannel clientChannel = channelManager.connect(client);
    client.connect(
        new InetSocketAddress(InetAddress.getLoopbackAddress(), server.socket().getLocalPort()));

    TestHelper.wait(listener.conOpen, 1, TestConstants.socketConnectTimeout);

    assertEquals(1, listener.conOpen.get());
    assertEquals(true, client.isConnected());
    assertEquals(0, listener.conClosed.get());
    assertEquals(0, listener.read.get());
    assertEquals(0, listener.write.get());

    channelManager.addChannel(clientChannel);
    channelManager.removeChannel(clientChannel);

    TestHelper.sleep(TestConstants.listenerTimeout);

    assertEquals(1, listener.conOpen.get());
    assertEquals(0, listener.conClosed.get());
    assertEquals(0, listener.read.get());
    assertEquals(0, listener.write.get());
  }
}
