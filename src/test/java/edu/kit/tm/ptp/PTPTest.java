package edu.kit.tm.ptp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import edu.kit.tm.ptp.connection.ConnectionManager;
import edu.kit.tm.ptp.utility.RNG;
import edu.kit.tm.ptp.utility.TestConstants;
import edu.kit.tm.ptp.utility.TestHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class offers JUnit testing for the PTP class.
 *
 * @author Simeon Andreev
 *
 */
public class PTPTest {

  /** The minimum length of the message used in the tests. */
  private static final int minMessageLength = 10;
  /** The maximum length of the message used in the tests. */
  private static final int maxMessageLength = 50;

  /** The first API wrapper object used in the self send test and in the ping-pong test. */
  private PTP client1 = null;
  /** The second API wrapper object used in the ping-pong test. */
  private PTP client2 = null;
  /** The message used in the tests. */
  private String testString = null;

  /*
   * Used by testSendClass().
   */
  static class Message {
    int id;

    // Necessary for kryo
    public Message() {
      id = -1;
    }

    public Message(int id) {
      this.id = id;
    }
  }


  /**
   * @throws IOException Propagates any IOException thrown by the API wrapper during construction.
   *
   * @see JUnit
   */
  @Before
  public void setUp() throws IOException {
    // Create a RNG.
    RNG random = new RNG();

    // Generate a random message within the length bounds.
    testString = random.string(minMessageLength, maxMessageLength);

    // Create the API wrapper objects.
    client1 = new PTP();
    client2 = new PTP();
  }

  /**
   * @see JUnit
   */
  @After
  public void tearDown() {
    // Clean up the APIs.
    client1.exit();
    client2.exit();
  }

  /**
   * Test for GetIdentifier().
   */
  @Test
  public void testGetIdentifier() throws IOException {
    client1.init();

    Identifier id1 = null;
    Identifier id2 = null;

    client1.reuseHiddenService();

    id1 = client1.getIdentifier();
    id2 = client1.getIdentifier();
    assertEquals(id1, id2);
  }

  /**
   * Test for fail handlers in SendListenerAdapter.
   */
  @Test
  public void testSendFail() throws IOException, InterruptedException {
    client1.init();

    // An atomic boolean used to check whether the sent message was received.
    final AtomicBoolean sendSuccess = new AtomicBoolean();

    // An atomic boolean used to check whether the sending failed.
    final AtomicBoolean sendFail = new AtomicBoolean();

    // two invalid identifiers
    Identifier invalidId1 = new Identifier("12345");
    Identifier invalidId2 = new Identifier("1234567812345678.onion");

    // helper class as we will be testing different addresses
    class TestSendFailHelper {

      private long returnedId = -1;

      public void run(Identifier id) {
        sendSuccess.set(false);
        sendFail.set(false);

        client1.setSendListener(new SendListener() {

          @Override
          public void messageSent(long id, Identifier destination, State state) {
            if (state == State.SUCCESS) {
              sendSuccess.set(true);
            } else {
              sendFail.set(true);
              returnedId = id;
            }
          }
        });

        // Send a message.
        final long timeout = 20 * 1000;
        final long msgId = client1.sendMessage(testString.getBytes(), id, timeout);
        // Wait for the sending result.
        final long waitStart = System.currentTimeMillis();
        while ((System.currentTimeMillis() - waitStart <= timeout + (10 * 1000))
            && !sendSuccess.get() && !sendFail.get()) {
          try {
            Thread.sleep(1 * 1000);
          } catch (InterruptedException e) {
            // Sleeping was interrupted. Do nothing.
          }
        }

        assertEquals("Received send success notification.", false, sendSuccess.get());
        // FIXME fails? mafl
        assertEquals("No failure notification received", true, sendFail.get());
        assertEquals(msgId, returnedId);
      }
    }

    // a random valid address
    Identifier offlineId = new Identifier("bwehoflnshqul42e.onion");

    TestSendFailHelper helper = new TestSendFailHelper();
    helper.run(invalidId1);
    helper.run(invalidId2);
    helper.run(offlineId);
  }

  /**
   * Test sending a message to an address managed by the same PTP instance, and receiving it. Fails
   * if the sent message was not received within a time interval, or if the received message does
   * not match the sent message.
   */
  @Test
  public void testSelfSend() throws IOException {
    client1.init();
    // Make sure there is a hidden service identifier.

    client1.reuseHiddenService();

    Identifier identifier = client1.getIdentifier();

    // An atomic boolean used to check whether the sent message was received yet.
    final AtomicBoolean received = new AtomicBoolean(false);
    final AtomicBoolean matches = new AtomicBoolean(false);

    // Set the listener.
    client1.setReceiveListener(new ReceiveListener() {
      @Override
      public void messageReceived(byte[] data, Identifier source) {
        System.out.println("Received message: " + new String(data));
        received.set(true);
        matches.set(new String(data).equals(testString));
      }
    });

    // Send the message.
    final AtomicBoolean sendSuccess = new AtomicBoolean(false);
    final long timeout = 180 * 1000;

    client1.setSendListener(new SendListener() {

      @Override
      public void messageSent(long id, Identifier destination, State state) {
        if (state == State.SUCCESS) {
          sendSuccess.set(true);
        }
      }

    });

    client1.sendMessage(testString.getBytes(), identifier, timeout);
    // Wait for the sending result.
    TestHelper.wait(sendSuccess, timeout + 5 * 1000);

    assertEquals("Sending the message via the client to the created identifier was not successful.",
        true, sendSuccess.get());

    // Wait (no more than 30 seconds) until the message was received.
    TestHelper.wait(received, 30 * 1000);

    assertEquals("Message not received.", true, received.get());
    assertEquals("Received message does not match sent message.", true, matches.get());
  }

  /**
   * Tests the API wrapper with a ping-pong between two API objects. Fails if a received message
   * does not match the first sent message, or if there is no real ping-pong, or if the number of
   * received message does not reach the maximum number of messages to receive.
   */
  @Test
  public void testPingPong() throws IOException, InterruptedException {
    client1.init();
    client2.init();
    // The maximum number of received messages during the ping-pong.
    final int max = 25;

    // Make sure there are hidden service identifiers for both instances.

    client1.reuseHiddenService();
    client2.reuseHiddenService();


    // Atomic variable for testing.
    final AtomicInteger counter1 = new AtomicInteger(0);
    final AtomicInteger counter2 = new AtomicInteger(0);
    final AtomicBoolean sendSuccess = new AtomicBoolean(false);
    final AtomicBoolean matchFail = new AtomicBoolean(false);
    final AtomicBoolean countingFail = new AtomicBoolean(false);

    // Set the listeners.
    client1.setReceiveListener(new ReceiveListener() {

      @Override
      public void messageReceived(byte[] data, Identifier source) {
        counter1.incrementAndGet();
        matchFail.set(!(new String(data).equals(testString)));
        if (counter1.get() - counter2.get() > 1) {
          countingFail.set(true);
        }
        client1.sendMessage(data, source, 10 * 1000);
      }
    });
    client2.setReceiveListener(new ReceiveListener() {
      @Override
      public void messageReceived(byte[] data, Identifier source) {
        counter2.incrementAndGet();
        matchFail.set(!(new String(data).equals(testString)));
        if (counter2.get() - counter1.get() > 1) {
          countingFail.set(true);
        }
        client2.sendMessage(data, source, 10 * 1000);
      }
    });

    client1.setSendListener(new SendListener() {

      @Override
      public void messageSent(long id, Identifier destination, State state) {
        if (state == State.SUCCESS) {
          sendSuccess.set(true);
        }
      }

    });

    // Send the initial ping-pong message.
    client1.sendMessage(testString.getBytes(), client2.getIdentifier(), 180 * 1000);

    // Wait for the sending result, to ensure first identifier is available.
    TestHelper.wait(sendSuccess, TestConstants.hiddenServiceSetupTimeout);

    assertEquals("Sending initial ping-pong message failed.", true, sendSuccess.get());

    // Wait for all ping-pong messages to arrive.
    final long start = System.currentTimeMillis();
    while (counter1.get() + counter2.get() < max
        && System.currentTimeMillis() - start < max * 10 * 1000 && !matchFail.get()
        && !countingFail.get()) {
      Thread.sleep(5 * 1000);
    }

    if (counter1.get() + counter2.get() < max) {
      fail("Maximum number of received messages not reached.");
    }

    assertEquals("An instance received a message that did not match the sent message.", false,
        matchFail.get());
    assertEquals("Weird ordering fail: one of the instances was 2 messages ahead.", false,
        countingFail.get());
  }

  /**
   * Tests sending a 16 MB message between two PTP instances. Fails if the sent message was not
   * received within a time interval, or if the received message does not match the sent message.
   * Warning: better deactivate logging of messages &lt;WARNING for this test.
   */
  @Test
  public void testSendBig() throws IOException {
    client1.init();
    client2.init();

    // Make sure both instances have hidden service identifiers.
    client1.reuseHiddenService();
    client2.reuseHiddenService();

    // Atomic flags for testing
    final AtomicBoolean sendSuccess = new AtomicBoolean(false);
    final AtomicBoolean receiveSuccess = new AtomicBoolean(false);
    final AtomicBoolean matches = new AtomicBoolean(false);
    final AtomicInteger failState = new AtomicInteger(-1);

    // create a ~16mb string
    StringBuilder sb = new StringBuilder(2 ^ 24);
    sb.append("x");
    for (int i = 0; i < 24; i++) {
      sb.append(sb.toString());
    }

    final String bigString = sb.toString();

    // Set the listener.
    client2.setReceiveListener(new ReceiveListener() {

      @Override
      public void messageReceived(byte[] data, Identifier source) {
        matches.set((new String(data)).equals(bigString));
        receiveSuccess.set(true);
      }
    });

    client1.setSendListener(new SendListener() {

      @Override
      public void messageSent(long id, Identifier destination, State state) {
        if (state == State.SUCCESS) {
          sendSuccess.set(true);
        } else {
          failState.set(state.ordinal());
        }
      }

    });

    final long timeout = 300 * 1000;
    // send the big message
    client1.sendMessage(bigString.getBytes(), client2.getIdentifier(), timeout);

    // Wait for the sending result
    long waitStart = System.currentTimeMillis();
    while (System.currentTimeMillis() - waitStart <= timeout + (5 * 1000) && !sendSuccess.get()
        && (failState.get() < 0)) {
      try {
        Thread.sleep(1 * 1000);
      } catch (InterruptedException e) {
        // Sleeping was interrupted. Do nothing.
      }
    }

    assertEquals("Sending failed: ", -1, failState.get());

    assertEquals("Sending timed out and this wasn't detected by sendListener.", true,
        sendSuccess.get());

    // Wait (no more than 2 minutes) until the message was received.
    TestHelper.wait(receiveSuccess, 120 * 1000);

    assertEquals("Received message does not match sent message.", true, matches.get());
  }

  /**
   * Tests registering and sending an arbitrary class between two PTP instances.
   */
  @Test
  public void testSendClass() throws IOException {
    client1.init();
    client2.init();

    final AtomicBoolean received = new AtomicBoolean(false);
    final AtomicBoolean messageMatches = new AtomicBoolean(false);
    final AtomicBoolean sourceMatches = new AtomicBoolean(false);

    client1.reuseHiddenService();
    client2.reuseHiddenService();

    final Identifier from = client1.getIdentifier();
    final Message toSend = new Message((int) (Math.random() * Integer.MAX_VALUE));

    class MessageListener implements MessageReceivedListener<Message> {
      @Override
      public void messageReceived(Message message, Identifier source) {
        messageMatches.set(toSend.id == message.id);
        sourceMatches.set(source.equals(from));
        received.set(true);
      }
    }

    SendReceiveListener listener = new SendReceiveListener();

    client1.setSendListener(listener);

    // Message has to be registered on both ends
    client1.registerClass(Message.class);
    client2.registerClass(Message.class);
    client2.setReceiveListener(Message.class, new MessageListener());

    client1.sendMessage(toSend, client2.getIdentifier());

    TestHelper.wait(listener.sent, 1, TestConstants.hiddenServiceSetupTimeout);
    TestHelper.wait(received, TestConstants.listenerTimeout);

    assertEquals(1, listener.sent.get());
    assertTrue(received.get());
    assertTrue(messageMatches.get());
    assertTrue(sourceMatches.get());
  }

  /**
   * Tests identifier reuse.
   */
  @Test
  public void testReuseHiddenService() throws IOException {
    client1.init();

    Identifier identifier1;
    Identifier identifier2;

    client1.reuseHiddenService();
    identifier1 = client1.getIdentifier();
    client1.reuseHiddenService();
    identifier2 = client1.getIdentifier();
    assertEquals(identifier1, identifier2);

    client1.exit();
    client1 = new PTP();
    client1.init();

    client1.reuseHiddenService();
    identifier2 = client1.getIdentifier();
    assertEquals(identifier1, identifier2);

    client1.createHiddenService();
    identifier1 = client1.getIdentifier();
    client1.reuseHiddenService();
    identifier2 = client1.getIdentifier();
    assertEquals(identifier1, identifier2);

    client1.exit();
    client1 = new PTP();
    client1.init();

    client1.createHiddenService();
    identifier1 = client1.getIdentifier();

    client1.exit();
    client1 = new PTP();
    client1.init();

    client1.reuseHiddenService();
    identifier2 = client1.getIdentifier();
    assertEquals(identifier1, identifier2);
  }

  /**
   * Tests identifier change.
   */
  @Test
  public void testCreateHiddenService() throws IOException {
    client1.init();

    Identifier identifier;
    Set<Identifier> pastIdentifiers = new HashSet<Identifier>();

    client1.createHiddenService();
    identifier = client1.getIdentifier();
    assertFalse(pastIdentifiers.contains(identifier));
    pastIdentifiers.add(identifier);

    client1.createHiddenService();
    identifier = client1.getIdentifier();
    assertFalse(pastIdentifiers.contains(identifier));
    pastIdentifiers.add(identifier);

    client1.exit();
    client1 = new PTP();
    client1.init();

    client1.createHiddenService();
    identifier = client1.getIdentifier();
    assertFalse(pastIdentifiers.contains(identifier));

    client1.reuseHiddenService();
    identifier = client1.getIdentifier();
    assertFalse(pastIdentifiers.contains(identifier));
    pastIdentifiers.add(identifier);

    client1.createHiddenService();
    identifier = client1.getIdentifier();
    assertFalse(pastIdentifiers.contains(identifier));

    client1.exit();
    client1 = new PTP();
    client1.init();

    client1.reuseHiddenService();
    identifier = client1.getIdentifier();
    assertFalse(pastIdentifiers.contains(identifier));
    pastIdentifiers.add(identifier);

    client1.exit();
    client1 = new PTP();
    client1.init();

    client1.createHiddenService();
    identifier = client1.getIdentifier();
    assertFalse(pastIdentifiers.contains(identifier));
  }
  
  @Test
  public void testSendGarbagePreAuth() throws IOException {
    client1.init();
    
    client1.reuseHiddenService();
    
    SendReceiveListener listener = new SendReceiveListener();
    
    client1.setReceiveListener(listener);
    
    Socket socket = new Socket();
    socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), client1.getLocalPort()),
        TestConstants.socketConnectTimeout);

    assertEquals(true, socket.isConnected());
    
    OutputStream os = socket.getOutputStream();
    byte[] testData = { 0x0, 0x1, 0x1A, (byte) 0xB4};
    os.write(testData);
    os.flush();
    
    TestHelper.sleep(TestConstants.listenerTimeout);
    socket.close();
    
    assertEquals(0, listener.received.get());
  }
  
  @Test
  public void testSendGarbagePostAuth() throws IOException {
    sendPostAuth(new byte[][] {new byte[] {0x1, (byte) 0xB2, 0x11, 0x7A, 0x0, 0x2, 0x3}});
  }
  
  @Test
  public void testSendEmptyByteArrayPostAuth() throws IOException {
    sendPostAuth(new byte[][]{new byte[] {}, new byte[] {0x0}});
  }
  
  private void sendPostAuth(byte[][] data) throws IOException {
    client1.init();
    client2.init();
    
    client1.reuseHiddenService();
    client2.reuseHiddenService();
    
    SendReceiveListener listener = new SendReceiveListener();
    
    client1.setSendListener(listener);
    client2.setReceiveListener(listener);
    
    assertNotNull(client2.getIdentifier());
    
    ConnectionManager cm1 = client1.connectionManager;
    
    for (byte[] array: data) {
      cm1.send(array, client2.getIdentifier(), -1);
    }
    
    TestHelper.wait(listener.sent, data.length, TestConstants.hiddenServiceSetupTimeout);
    assertEquals(data.length, listener.sent.get());
    
    TestHelper.sleep(TestConstants.listenerTimeout);
    assertEquals(0, listener.received.get());
  }
  
  @Test
  public void testDeleteHiddenService() throws IOException {
    client1.init();
    client1.reuseHiddenService();
    
    File hsDir = new File(client1.getHiddenServiceDirectory());
    
    client1.exit();
    client1.deleteHiddenService();
    
    assertFalse(hsDir.exists());
  }
  
  @Test (expected = IllegalArgumentException.class)
  public void testSetListenerUnregisteredClass() throws IOException {
    client1.init();
    
    client1.setReceiveListener(Message.class, new MessageReceivedListener<Message>() {
      @Override
      public void messageReceived(Message message, Identifier source) {
      }
    });
  }
  
  @Test (expected = IllegalArgumentException.class)
  public void testSendMessageNull() throws IOException {
    client1.init();
    client1.reuseHiddenService();
    
    assertNotNull(client1.getIdentifier());
    
    client1.sendMessage(null, client1.getIdentifier());
  }
  
  @Test (expected = IllegalArgumentException.class)
  public void testSendMessageNull2() throws IOException {
    client1.init();
    client1.reuseHiddenService();
    
    assertNotNull(client1.getIdentifier());
    
    client1.sendMessage((Object)null, client1.getIdentifier());
  }
  
  @Test (expected = IllegalArgumentException.class)
  public void testSendMessageNull3() throws IOException {
    client1.init();
    client1.reuseHiddenService();
    
    assertNotNull(client1.getIdentifier());
    
    client1.sendMessage(null, client1.getIdentifier(), -1);
  }
  
  @Test (expected = IllegalArgumentException.class)
  public void testSendMessageNull4() throws IOException {
    client1.init();
    client1.reuseHiddenService();
    
    assertNotNull(client1.getIdentifier());
    
    client1.sendMessage((Object)null, client1.getIdentifier(), -1);
  }
}
