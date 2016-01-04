package edu.kit.tm.ptp;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import edu.kit.tm.ptp.raw.Configuration;
import edu.kit.tm.ptp.serialization.Serializer;
import edu.kit.tm.ptp.utility.Constants;

public class ConnectionManagerTest {
  class Listener implements SendListener, ReceiveListener {
    private AtomicInteger sent = new AtomicInteger(0);
    private AtomicInteger received = new AtomicInteger(0);
    public byte[] receivedData;
    public Identifier source;
    public Identifier destination;
    public long id;
    public State state;

    @Override
    public void messageReceived(byte[] data, Identifier source) {
      receivedData = data;
      this.source = source;
      received.incrementAndGet();
    }

    @Override
    public void messageSent(long id, Identifier destination, State state) {
      this.id = id;
      this.destination = destination;
      this.state = state;
      sent.incrementAndGet();
    }
  }

  @Test
  public void testStartBindServer() throws IOException {
    ConnectionManager manager = new ConnectionManager(Constants.localhost, 1000, 1001);
    manager.start();
    int port = manager.startBindServer();

    int timeout = 5 * 1000;

    Socket socket = new Socket();
    socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), timeout);

    assertEquals(true, socket.isConnected());
    socket.close();
    manager.stop();
  }

  @Test
  public void testSend() throws IOException {
    Listener listener = new Listener();
    PTP ptp = new PTP();
    ptp.createHiddenService();

    Configuration config = ptp.getConfiguration();

    ConnectionManager manager = new ConnectionManager(Constants.localhost,
        config.getTorSOCKSProxyPort(), config.getHiddenServicePort());
    manager.setSendListener(listener);
    manager.setLocalIdentifier(new Identifier("bla.onion"));
    manager.setSerializer(new Serializer());
    manager.start();
    
    long timeout = 120 * 1000;
    
    byte[] data = new byte[] { 0x0, 0x1, 0x2, 0x3 };
    long id = manager.send(data, ptp.getIdentifier(), timeout);
    
    TestHelper.wait(listener.sent, 1, timeout);
    
    assertEquals(1, listener.sent.get());
    
    assertEquals(id, listener.id);
    assertEquals(SendListener.State.SUCCESS, listener.state);
    assertEquals(ptp.getIdentifier(), listener.destination);
  }

}
