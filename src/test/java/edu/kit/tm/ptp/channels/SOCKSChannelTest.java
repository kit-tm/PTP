package edu.kit.tm.ptp.channels;

import static org.junit.Assert.*;

import edu.kit.tm.ptp.utility.Constants;
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

public class SOCKSChannelTest {
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

  @Test(expected = IllegalStateException.class)
  public void testaddMessageBeforeConnecting() throws IOException {
    SocketChannel client = SocketChannel.open();
    client.configureBlocking(false);
    if (!client.connect(
        new InetSocketAddress(InetAddress.getLoopbackAddress(), server.socket().getLocalPort()))) {
      TestHelper.sleep(TestConstants.socketConnectTimeout);
      client.finishConnect();
    }

    assertEquals(true, client.isConnected());
    ChannelManager manager = new ChannelManager(new Listener());
    SOCKSChannel channel = new SOCKSChannel(client, manager);
    byte[] data = new byte[] {0x0};
    channel.addMessage(data, 10L);
  }

}
