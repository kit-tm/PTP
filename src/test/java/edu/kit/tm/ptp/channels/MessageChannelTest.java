package edu.kit.tm.ptp.channels;

import static org.junit.Assert.*;

import edu.kit.tm.ptp.TestHelper;
import edu.kit.tm.ptp.utility.Constants;

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

  /*@Test
  public void testReadWrite() throws IOException {
    Listener listener = new Listener();
    ChannelManager manager = new ChannelManager(listener);
    manager.start();

    manager.addServerSocket(server);

    SocketChannel client = SocketChannel.open();
    manager.connect(client);
    client.connect(
        new InetSocketAddress(InetAddress.getLoopbackAddress(), server.socket().getLocalPort()));

    long timeout = 5 * 1000;

    TestHelper.wait(listener.conOpen, 2, timeout);

    assertEquals(2, listener.conOpen.get());
    assertEquals(0, listener.conClosed.get());
    assertEquals(0, listener.read.get());
    assertEquals(0, listener.write.get());

    assertEquals(2, listener.passedChannels.size());
    MessageChannel c1 = listener.passedChannels.get(0);

    int length = 1024;
    byte[] data = new byte[1024]; // 1kB

    for (int i = 0; i < length; i++) {
      data[i] = 0x00;
    }

    long id = (long) (Math.random() * (double) Long.MAX_VALUE);

    c1.addMessage(data, id);

    TestHelper.wait(listener.write, 1, timeout);

    assertEquals(2, listener.conOpen.get());
    assertEquals(0, listener.conClosed.get());
    assertEquals(1, listener.read.get());
    assertEquals(1, listener.write.get());

    assertEquals(data, listener.passedBytes);
    assertEquals(id, listener.passedId);

    client.close();
    assertEquals(2, listener.conOpen.get());
    assertEquals(2, listener.conClosed.get());
    assertEquals(1, listener.read.get());
    assertEquals(1, listener.write.get());
    manager.stop();
  }*/
  
  @Test
  public void testReadWrite() throws IOException {
    SocketChannel client = SocketChannel.open();
    client.configureBlocking(false);
    client.connect(
        new InetSocketAddress(InetAddress.getLoopbackAddress(), server.socket().getLocalPort()));
    
    SocketChannel serverChannel = server.accept();
    assertNotEquals(null, serverChannel);
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

    long id = (long) (Math.random() * (double) Long.MAX_VALUE);

    c1.addMessage(data, id);

    long timeout = 5 * 1000;
    TestHelper.wait(listener.read, 1, timeout);

    assertEquals(1, listener.read.get());
    assertEquals(1, listener.write.get());

    assertEquals(data, listener.passedBytes);
    assertEquals(id, listener.passedId);
    assertEquals(c1, listener.destination);
    assertEquals(c2, listener.source);

    client.close();
    assertEquals(1, listener.read.get());
    assertEquals(1, listener.write.get());
    assertEquals(1, listener.conClosed.get());
    assertEquals(client, listener.passedChannel);
  }

  @Test
  public void testGetChannel() throws IOException {
    SocketChannel client = SocketChannel.open();
    client.configureBlocking(false);
    if (!client.connect(
        new InetSocketAddress(InetAddress.getLoopbackAddress(), server.socket().getLocalPort()))) {
      TestHelper.sleep(5 * 1000);
      client.finishConnect();
    }
    
    assertEquals(true, client.isConnected());
    ChannelManager manager = new ChannelManager(new Listener());
    MessageChannel channel = new MessageChannel(client, manager);
    assertEquals(client, channel.getChannel());
  }

}
