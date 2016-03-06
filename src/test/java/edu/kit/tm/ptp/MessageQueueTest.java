package edu.kit.tm.ptp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import edu.kit.tm.ptp.utility.TestConstants;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

/**
 * Class to test receiving messages of an own class type through queues offered by PTP.
 * 
 * @author Timon Hackenjos
 *
 */
public class MessageQueueTest {
  private PTP ptp;
  private PTP ptp2;

  private static final class TestMessage {
    public TestMessage() {}

    public TestMessage(String data, int value, boolean b) {
      this.data = data;
      this.value = value;
      this.b = b;
    }

    public String data;
    public int value;
    public boolean b;
  }

  @Before
  public void setUp() throws Exception {
    ptp = new PTP();
    ptp2 = new PTP();
    ptp.init();
    ptp2.init();
  }

  @After
  public void tearDown() throws Exception {
    ptp.exit();
    ptp2.exit();
  }

  @Test
  public void testPollMessageAPI1() throws IOException, InterruptedException {
    ptp.reuseHiddenService();
    ptp2.reuseHiddenService();
    ptp.registerMessageQueue(TestMessage.class);
    ptp2.registerMessageQueue(TestMessage.class);

    assertNotEquals(null, ptp2.getIdentifier());

    TestMessage message = new TestMessage("Hallo", 123, false);
    ptp.sendMessage(message, ptp2.getIdentifier());

    IMessageQueue queue = ptp2.getMessageQueue();

    long start = System.currentTimeMillis();

    while (!queue.hasMessage(TestMessage.class)
        && System.currentTimeMillis() - start < TestConstants.hiddenServiceSetupTimeout) {
      Thread.sleep(1000);
    }

    pollMessageAPI1(message, queue);
  }

  @Test
  public void testPollMessageAPI2() throws IOException, InterruptedException {
    ptp.reuseHiddenService();
    ptp2.reuseHiddenService();
    ptp.registerMessageQueue(TestMessage.class);
    ptp2.registerMessageQueue(TestMessage.class);

    assertNotEquals(null, ptp2.getIdentifier());

    TestMessage message = new TestMessage("Hallo", 123, false);
    ptp.sendMessage(message, ptp2.getIdentifier());

    IMessageQueue queue = ptp2.getMessageQueue();

    long start = System.currentTimeMillis();

    while (!queue.hasMessage(TestMessage.class)
        && System.currentTimeMillis() - start < TestConstants.hiddenServiceSetupTimeout) {
      Thread.sleep(1000);
    }
    
    pollMessageAPI2(message, queue);
  }

  @Test
  public void testPollMessageListener() throws IOException, InterruptedException {
    ptp.reuseHiddenService();
    ptp2.reuseHiddenService();
    ptp.registerListener(TestMessage.class, new MessageReceivedListener<TestMessage>() {
      @Override
      public void messageReceived(TestMessage message, Identifier source) {
      }
    });
    ptp2.registerMessageQueue(TestMessage.class);

    assertNotEquals(null, ptp2.getIdentifier());

    TestMessage message = new TestMessage("Hallo", 123, false);
    ptp.sendMessage(message, ptp2.getIdentifier());

    IMessageQueue queue = ptp2.getMessageQueue();

    long start = System.currentTimeMillis();

    while (!queue.hasMessage(TestMessage.class)
        && System.currentTimeMillis() - start < TestConstants.hiddenServiceSetupTimeout) {
      Thread.sleep(1000);
    }

    pollMessageAPI2(message, queue);
  }
  
  private void pollMessageAPI1(TestMessage sent, IMessageQueue queue) {
    assertEquals(true, queue.hasMessage(TestMessage.class));

    QueuedMessage<TestMessage> received =
        queue.pollMessage(TestMessage.class, new QueuedMessage<TestMessage>());

    TestMessage receivedMessage = received.data;

    assertEquals(ptp.getIdentifier(), received.source);

    assertEquals(sent.data, receivedMessage.data);
    assertEquals(sent.value, receivedMessage.value);
    assertEquals(sent.b, receivedMessage.b);
  }
  
  private void pollMessageAPI2(TestMessage sent, IMessageQueue queue) {
    assertEquals(true, queue.hasMessage(TestMessage.class));

    TestMessage receivedMessage = queue.pollMessage(TestMessage.class);
    Identifier source = queue.getMessageSource(receivedMessage);

    assertEquals(ptp.getIdentifier(), source);

    assertEquals(sent.data, receivedMessage.data);
    assertEquals(sent.value, receivedMessage.value);
    assertEquals(sent.b, receivedMessage.b);
  }

}
