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

public class SOCKSChannelTest {
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

  @Test (expected = IllegalStateException.class)
  public void testaddMessageBeforeConnecting() throws IOException {
    SocketChannel client = SocketChannel.open();
    client.configureBlocking(false);
    if (!client.connect(
        new InetSocketAddress(InetAddress.getLoopbackAddress(), server.socket().getLocalPort()))) {
      TestHelper.sleep(5 * 1000);
      client.finishConnect();
    }
    
    assertEquals(true, client.isConnected());
    ChannelManager manager = new ChannelManager(new Listener());
    SOCKSChannel channel = new SOCKSChannel(client, manager);
    byte[] data = new byte[] { 0x0 };
    channel.addMessage(data, 10L);
  }

}
