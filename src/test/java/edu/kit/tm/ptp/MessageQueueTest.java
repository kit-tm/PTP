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
    public String data;
    public int data2;
    public boolean data3;
    
    public TestMessage() {}

    public TestMessage(String data, int data2, boolean data3) {
      this.data = data;
      this.data2 = data2;
      this.data3 = data3;
    }
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
  public void testPollMessageAPI2() throws IOException, InterruptedException {
    ptp.reuseHiddenService();
    ptp2.reuseHiddenService();
    ptp.registerMessageQueue(TestMessage.class);
    ptp2.registerMessageQueue(TestMessage.class);

    assertNotEquals(null, ptp2.getIdentifier());

    TestMessage message = new TestMessage("Hallo", 123, false);
    ptp.sendMessage(message, ptp2.getIdentifier());
    
    IMessageQueue<TestMessage> receiveQueue = ptp2.getMessageQueue(TestMessage.class);

    long start = System.currentTimeMillis();

    while (!receiveQueue.hasMessage()
        && System.currentTimeMillis() - start < TestConstants.hiddenServiceSetupTimeout) {
      Thread.sleep(1000);
    }
    
    pollMessageIterator(message, receiveQueue);
  }
  
  @Test
  public void testPollIterator() throws IOException, InterruptedException {
    ptp.reuseHiddenService();
    ptp2.reuseHiddenService();
    ptp.registerMessageQueue(TestMessage.class);
    ptp2.registerMessageQueue(TestMessage.class);

    assertNotEquals(null, ptp2.getIdentifier());

    TestMessage message = new TestMessage("Hallo", 123, false);
    ptp.sendMessage(message, ptp2.getIdentifier());
    
    IMessageQueue<TestMessage> receiveQueue = ptp2.getMessageQueue(TestMessage.class);

    long start = System.currentTimeMillis();

    while (!receiveQueue.hasMessage()
        && System.currentTimeMillis() - start < TestConstants.hiddenServiceSetupTimeout) {
      Thread.sleep(1000);
    }
    
    pollMessageIterator(message, receiveQueue);
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
    
    IMessageQueue<TestMessage> receiveQueue = ptp2.getMessageQueue(TestMessage.class);

    long start = System.currentTimeMillis();

    while (!receiveQueue.hasMessage()
        && System.currentTimeMillis() - start < TestConstants.hiddenServiceSetupTimeout) {
      Thread.sleep(1000);
    }

    pollMessageIterator(message, receiveQueue);
  }


  private void pollMessageIterator(TestMessage sent, IMessageQueue<TestMessage> queue) {
    assertEquals(true, queue.hasMessage());
    
    QueuedMessage<TestMessage> message = queue.pollMessage();
    TestMessage receivedMessage = message.data;
    Identifier source = message.source;
    
    assertEquals(ptp.getIdentifier(), source);
    
    assertEquals(sent.data, receivedMessage.data);
    assertEquals(sent.data2, receivedMessage.data2);
    assertEquals(sent.data3, receivedMessage.data3);
    
    assertEquals(false, queue.hasMessage());
  }
}
