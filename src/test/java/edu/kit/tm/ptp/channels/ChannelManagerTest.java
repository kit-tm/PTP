package edu.kit.tm.ptp.channels;

import static org.junit.Assert.*;

import edu.kit.tm.ptp.PTP;
import edu.kit.tm.ptp.TestHelper;
import edu.kit.tm.ptp.utility.Constants;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test class for the ChannelManager.
 * 
 * @author Timon Hackenjos
 *
 */
public class ChannelManagerTest {
  private ServerSocketChannel server = null;

  @Before
  public void setUp() {
    try {
      server = ServerSocketChannel.open();
      server.socket()
          .bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), Constants.anyport));
      server.configureBlocking(false);

    } catch (IOException ioe) {
      fail("An error occurred while setting up a ServerSocketChannel: " + ioe.getMessage());
    }
  }

  @After
  public void tearDown() {
    if (server != null) {
      try {
        server.close();
      } catch (IOException ioe) {
        // Do nothing
      }
    }
  }

  @Test
  public void testaddServerSocket() throws IOException {
    Listener listener = new Listener();
    ChannelManager channelManager = new ChannelManager(listener);
    channelManager.start();
    channelManager.addServerSocket(server);

    Socket client = null;
    int timeout = 5 * 1000;

    client = new Socket();
    client.connect(
        new InetSocketAddress(InetAddress.getLoopbackAddress(), server.socket().getLocalPort()),
        timeout);

    TestHelper.wait(listener.conOpen, 1, timeout);
    client.close();

    assertEquals(1, listener.conOpen.get());
    assertEquals(0, listener.conClosed.get());
    assertEquals(0, listener.read.get());
    assertEquals(0, listener.write.get());

    channelManager.stop();
  }

  @Test
  public void testConnect() throws IOException {
    Listener listener = new Listener();
    ChannelManager channelManager = new ChannelManager(listener);
    channelManager.start();

    SocketChannel client = null;


    client = SocketChannel.open();
    MessageChannel clientChannel = channelManager.connect(client);
    client.connect(
        new InetSocketAddress(InetAddress.getLoopbackAddress(), server.socket().getLocalPort()));

    long timeout = 5 * 1000;
    TestHelper.wait(listener.conOpen, 1, timeout);
    
    channelManager.addChannel(clientChannel);
    
    assertEquals(true, client.isConnected());
    assertEquals(1, listener.conOpen.get());
    assertEquals(0, listener.conClosed.get());
    assertEquals(0, listener.read.get());
    assertEquals(0, listener.write.get());

    channelManager.removeChannel(clientChannel);
    
    TestHelper.wait(listener.conClosed, 1, timeout);

    assertEquals(1, listener.conOpen.get());
    assertEquals(1, listener.conClosed.get());
    assertEquals(0, listener.read.get());
    assertEquals(0, listener.write.get());
    
    channelManager.stop();
  }



  @Test
  public void testConnectThroughSOCKS() throws IOException {
    Listener socks = new Listener();
    ChannelManager channelManager = new ChannelManager(socks);
    channelManager.start();

    PTP ptp = new PTP();
    ptp.createHiddenService();

    SocketChannel client = null;

    client = SocketChannel.open();
    channelManager.connect(client);
    client.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(),
        ptp.getConfiguration().getTorSOCKSProxyPort()));

    long timeout = 5 * 1000;

    TestHelper.wait(socks.conOpen, 1, timeout);

    assertEquals(1, socks.conOpen);
    assertEquals(0, socks.conClosed);
    assertEquals(0, socks.read);
    assertEquals(0, socks.write);

    SOCKSChannel socksChannel = new SOCKSChannel(socks.passedChannel, channelManager);
    socksChannel.connetThroughSOCKS(ptp.getIdentifier().toString(),
        ptp.getConfiguration().getHiddenServicePort());
    channelManager.addChannel(socksChannel);

    TestHelper.wait(socks.conOpen, 2, timeout);

    assertEquals(2, socks.conOpen);
    assertEquals(0, socks.read);
    assertEquals(0, socks.write);
    assertEquals(socksChannel, socks.passedChannel);
    assertEquals(0, socks.conClosed);

    client.close();

    assertEquals(1, socks.conClosed);
    assertEquals(socksChannel, socks.passedChannel);

    channelManager.stop();
  }

  @Test
  public void testRemoveChannel() throws IOException {
    Listener listener = new Listener();
    ChannelManager channelManager = new ChannelManager(listener);
    channelManager.start();

    SocketChannel client = null;

    client = SocketChannel.open();
    MessageChannel clientChannel = channelManager.connect(client);
    client.connect(
        new InetSocketAddress(InetAddress.getLoopbackAddress(), server.socket().getLocalPort()));
    
    long timeout = 5 * 1000;
    TestHelper.wait(listener.conOpen, 1, timeout);
    assertEquals(1, listener.conOpen.get());
    assertEquals(true, client.isConnected());

    channelManager.addChannel(clientChannel);

    assertEquals(0, listener.conClosed.get());
    assertEquals(0, listener.read.get());
    assertEquals(0, listener.write.get());

    channelManager.removeChannel(clientChannel);
    assertEquals(1, listener.conOpen.get());
    assertEquals(1, listener.conClosed.get());
    assertEquals(0, listener.read.get());
    assertEquals(0, listener.write.get());
  }
}
