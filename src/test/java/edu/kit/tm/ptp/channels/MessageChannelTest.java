package edu.kit.tm.ptp.channels;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

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
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class MessageChannelTest {
  private ServerSocketChannel server = null;

  @Before
  public void setUp() throws IOException {
    server = ServerSocketChannel.open();
    server.socket()
        .bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), Constants.anyport));
    server.configureBlocking(false);
  }

  @After
  public void tearDown() throws IOException {
    if (server != null) {
      server.close();
    }
  }

  @Test
  public void testReadWrite() throws IOException {
    SocketChannel client = SocketChannel.open();
    client.configureBlocking(false);
    client.connect(
        new InetSocketAddress(InetAddress.getLoopbackAddress(), server.socket().getLocalPort()));

    SocketChannel serverChannel = server.accept();
    assertNotEquals(null, serverChannel);
    serverChannel.configureBlocking(false);
    client.finishConnect();

    Listener listener = new Listener();
    ChannelManager manager = new ChannelManager(listener);
    MessageChannel c1 = new MessageChannel(client, manager);
    MessageChannel c2 = new MessageChannel(serverChannel, manager);

    int length = 1024;
    byte[] data = new byte[1024]; // 1kB

    for (int i = 0; i < length; i++) {
      data[i] = 0x00;
    }

    // Random id
    long id = (long) (Math.random() * (double) Long.MAX_VALUE);

    c1.addMessage(data, id);

    for (int i = 0; i < 10; i++) {
      c1.write();
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        // Do nothing
      }
    }

    for (int i = 0; i < 10; i++) {
      c2.read();
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        // Do nothing
      }
    }
    TestHelper.wait(listener.read, 1, TestConstants.listenerTimeout);

    assertEquals(1, listener.read.get());
    assertEquals(1, listener.write.get());

    assertArrayEquals(data, listener.getPassedBytes());
    assertEquals(id, listener.getPassedId());
    assertEquals(c1, listener.getDestination());
    assertEquals(c2, listener.getSource());

    client.close();
    c2.read();
    assertEquals(1, listener.read.get());
    assertEquals(1, listener.write.get());
    assertEquals(1, listener.conClosed.get());
    assertEquals(c2, listener.getPassedChannel());
  }

  @Test
  public void testGetChannel() throws IOException {
    SocketChannel client = SocketChannel.open();
    client.configureBlocking(false);
    if (!client.connect(
        new InetSocketAddress(InetAddress.getLoopbackAddress(), server.socket().getLocalPort()))) {
      TestHelper.sleep(TestConstants.socketConnectTimeout);
      client.finishConnect();
    }

    assertEquals(true, client.isConnected());
    ChannelManager manager = new ChannelManager(new Listener());
    MessageChannel channel = new MessageChannel(client, manager);
    assertEquals(client, channel.getChannel());
  }

}
