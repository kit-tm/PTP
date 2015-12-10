package edu.kit.tm.ptp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import edu.kit.tm.ptp.Identifier;
import edu.kit.tm.ptp.PTP;
import edu.kit.tm.ptp.ReceiveListener;
import edu.kit.tm.ptp.SendListener;
import edu.kit.tm.ptp.raw.connection.RNG;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
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
  public void testGetIdentifier() {

    Identifier id1 = null;
    Identifier id2 = null;

    try {
      client1.createHiddenService();
    } catch (IOException e) {
      fail("Caught an IOException while creating the hidden service identifier: " + e.getMessage());
    }

    id1 = client1.getIdentifier();
    id2 = client1.getIdentifier();
    assertEquals(id1, id2);
  }

  /**
   * Test for fail handlers in SendListenerAdapter.
   */
  @Test
  public void testSendFail() {

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
        while ((System.currentTimeMillis() - waitStart <= timeout + (5 * 1000))
            && !sendSuccess.get() && !sendFail.get()) {
          try {
            Thread.sleep(1 * 1000);
          } catch (InterruptedException e) {
            // Sleeping was interrupted. Do nothing.
          }
        }

        if (sendSuccess.get()) {
          fail("Received send success notification.");
        }
        if (!sendFail.get()) {
          fail("No failure notification received");
        }
        if (returnedId != msgId) {
          fail("Notifier id " + returnedId + " does not equal id returned by send" + msgId);
        }
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
   * Test sending a message to an address managed by the same PTP instance, and receiving it.
   * Fails if the sent message was not received within a time interval, or if the received message
   * does not match the sent message.
   */
  @Test
  public void testSelfSend() {

    // Make sure there is a hidden service identifier.
    try {
      client1.reuseHiddenService();
    } catch (IOException e) {
      fail("Caught an IOException while creating the hidden service identifier: " + e.getMessage());
    }
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
    final long waitStart = System.currentTimeMillis();
    while (System.currentTimeMillis() - waitStart <= timeout + (5 * 1000) && !sendSuccess.get()) {
      try {
        Thread.sleep(1 * 1000);
      } catch (InterruptedException e) {
        // Sleeping was interrupted. Do nothing.
      }
    }
    if (!sendSuccess.get()) {
      fail("Sending the message via the client to the created identifier was not successful.");
    }

    // Wait (no more than 30 seconds) until the message was received.
    final long start = System.currentTimeMillis();
    while (!received.get()) {
      try {
        Thread.sleep(3 * 1000);
        if (System.currentTimeMillis() - start > 30 * 1000) {
          fail("Connecting to the created identifier took too long.");
        }
      } catch (InterruptedException e) {
        // Waiting was interrupted. Do nothing.
      }
    }

    if (!received.get()) {
      fail("Message not received.");
    }

    if (!matches.get()) {
      fail("Received message does not match sent message.");
    }
  }

  /**
   * Tests the API wrapper with a ping-pong between two API objects.
   * Fails if a received message does not match the first sent message, or if there is no real
   * ping-pong, or if the number of received message does not reach the maximum number of messages
   * to receive.
   */
  @Test
  public void testPingPong() {
    // The maximum number of received messages during the ping-pong.
    final int max = 25;

    // Make sure there are hidden service identifiers for both instances.
    try {
      client1.reuseHiddenService();
      client2.reuseHiddenService();
    } catch (IOException e) {
      fail("Caught an IOException while creating the identifiers: " + e.getMessage());
    }

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
    Long waitStart = System.currentTimeMillis();
    while (System.currentTimeMillis() - waitStart <= 185 * 1000 && !sendSuccess.get()) {
      try {
        Thread.sleep(1 * 1000);
      } catch (InterruptedException e) {
        // Sleeping was interrupted. Do nothing.
      }
    }
    if (!sendSuccess.get()) {
      fail("Sending initial ping-pong message failed.");
    }

    // Wait for all ping-pong messages to arrive.
    final long start = System.currentTimeMillis();
    while (counter1.get() + counter2.get() < max && System.currentTimeMillis() - start < 300 * 1000
        && !matchFail.get() && !countingFail.get()) {
      try {
        Thread.sleep(5 * 1000);
      } catch (InterruptedException e) {
        // Waiting was interrupted. Do nothing.
      }
    }
    if (counter1.get() + counter2.get() < max) {
      fail("Maximum number of received messages not reached.");
    }

    if (matchFail.get()) {
      fail("An instance received a message that did not match the sent message.");
    }

    if (countingFail.get()) {
      fail("Weird ordering fail: one of the instances was 2 messages ahead.");
    }
  }

  /**
   * Tests sending a 16 MB message between two PTP instances.
   * Fails if the sent message was not received within a time interval, or if the received message
   * does not match the sent message. Warning: better deactivate logging of messages &lt;WARNING for
   * this test.
   */
  @Test
  public void testSendBig() {

    // Make sure both instances have hidden service identifiers.
    try {
      client1.reuseHiddenService();
      client2.reuseHiddenService();
    } catch (IOException e) {
      fail("Caught an IOException while creating the identifiers: " + e.getMessage());
    }

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
    if (failState.get() >= 0) {
      fail("Sending failed: " + SendListener.State.values()[failState.get()].toString());
    }
    if (!sendSuccess.get()) {
      fail("Sending timed out and this wasn't detected by sendListener.");
    }

    // Wait (no more than 2 minutes) until the message was received.
    waitStart = System.currentTimeMillis();
    while (!receiveSuccess.get()) {
      try {
        Thread.sleep(3 * 1000);
        if (System.currentTimeMillis() - waitStart > 120 * 1000) {
          fail("SendingListener reported success but message not received after 2 minutes.");
        }
      } catch (InterruptedException e) {
        // Waiting was interrupted. Do nothing.
      }
    }
    if (!matches.get()) {
      fail("Received message does not match sent message.");
    }
  }
}
