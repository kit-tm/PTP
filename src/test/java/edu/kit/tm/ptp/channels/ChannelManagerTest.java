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

  class Listener implements ChannelListener {
    public MessageChannel passedChannel;
    public AtomicInteger conOpen = new AtomicInteger(0);
    public AtomicInteger conClosed = new AtomicInteger(0);
    public AtomicInteger other = new AtomicInteger(0);

    @Override
    public void messageSent(long id, MessageChannel destination) {
      other.incrementAndGet();
    }

    @Override
    public void messageReceived(byte[] data, MessageChannel source) {
      other.incrementAndGet();
    }

    @Override
    public void channelOpened(MessageChannel channel) {
      conOpen.incrementAndGet();
      passedChannel = channel;
    }

    @Override
    public void channelClosed(MessageChannel channel) {
      conClosed.incrementAndGet();
      passedChannel = channel;
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
    long start = System.currentTimeMillis();

    try {
      client = new Socket();
      client.connect(
          new InetSocketAddress(InetAddress.getLoopbackAddress(), server.socket().getLocalPort()),
          timeout);
    } finally {
      if (client != null) {
        try {
          client.close();
        } catch (IOException ioe) {
          // Do nothing
        }
      }
    }

    while (listener.conOpen.get() == 0 && (System.currentTimeMillis() - start < timeout)) {
      try {
        Thread.sleep(1 * 1000);
      } catch (InterruptedException e) {
        // Sleeping was interrupted. Do nothing.
      }
    }

    channelManager.stop();

    assertEquals(1, listener.conOpen.get());
    assertEquals(1, listener.conClosed.get());
    assertEquals(0, listener.other.get());
  }

  @Test
  public void testConnect() throws IOException {
    Listener listener = new Listener();
    ChannelManager channelManager = new ChannelManager(listener);
    channelManager.start();

    SocketChannel client = null;

    try {
      client = SocketChannel.open();
      client.configureBlocking(false);
      channelManager.connect(client);
      client.connect(
          new InetSocketAddress(InetAddress.getLoopbackAddress(), server.socket().getLocalPort()));
    } finally {
      if (client != null) {
        try {
          client.close();
        } catch (IOException ioe) {
          // Do nothing
        }
      }
    }

    long timeout = 5 * 1000;
    TestHelper.wait(listener.conOpen, 1, timeout);

    assertEquals(1, listener.conOpen.get());
    assertEquals(0, listener.conClosed.get());
    assertEquals(0, listener.other.get());

    channelManager.stop();

    if (listener.conOpen.get() > 0) {
      try {
        client.finishConnect();
        client.close();
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }
    }
  }



  @Test
  public void testConnectThroughSOCKS() throws IOException {
    Listener socks = new Listener();
    ChannelManager channelManager = new ChannelManager(socks);
    channelManager.start();

    PTP ptp = new PTP();
    ptp.reuseHiddenService();

    SocketChannel client = null;

    try {
      client = SocketChannel.open();
      client.configureBlocking(false);
      channelManager.connect(client);
      client.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(),
          ptp.getConfiguration().getTorSOCKSProxyPort()));
    } finally {
      if (client != null) {
        try {
          client.close();
        } catch (IOException ioe) {
          // Do nothing
        }
      }
    }

    long timeout = 5 * 1000;

    TestHelper.wait(socks.conOpen, 1, timeout);

    assertEquals(1, socks.conOpen);
    assertEquals(0, socks.conClosed);
    assertEquals(0, socks.other);

    client.finishConnect();

    SOCKSChannel socksChannel = new SOCKSChannel(socks.passedChannel);
    socksChannel.connetThroughSOCKS(ptp.getIdentifier().toString(),
        ptp.getConfiguration().getHiddenServicePort());
    channelManager.addChannel(socksChannel);

    TestHelper.wait(socks.conOpen, 2, timeout);

    assertEquals(2, socks.conOpen);
    assertEquals(0, socks.other);
    assertEquals(socksChannel, socks.passedChannel);
    assertEquals(0, socks.conClosed);

    client.close();

    assertEquals(1, socks.conClosed);
    assertEquals(socksChannel, socks.passedChannel);

    channelManager.stop();
  }
}
