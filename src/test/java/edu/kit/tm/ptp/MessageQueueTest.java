package edu.kit.tm.ptp;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import edu.kit.tm.ptp.utility.Constants;
import edu.kit.tm.ptp.utility.TestConstants;
import edu.kit.tm.ptp.utility.TestHelper;

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
  private static final byte[] testData = {(byte) 0xFF, 0x0, 0x1A};

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
    ptp = new PTP(true);
    ptp2 = new PTP(true);
    ptp.init();
    ptp2.init();
  }

  @After
  public void tearDown() throws Exception {
    ptp.exit();
    ptp2.exit();
  }
  
  @Test
  public void testPollTestMessageType() throws IOException, InterruptedException {
    ptp.registerClass(TestMessage.class);
    ptp2.registerClass(TestMessage.class);

    ptp.reuseHiddenService();
    ptp2.reuseHiddenService();

    ptp.enableMessageQueue(TestMessage.class);
    ptp2.enableMessageQueue(TestMessage.class);

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
    TestMessage receivedMessage = message.getData();
    Identifier source = message.getSource();
    
    assertEquals(ptp.getIdentifier(), source);
    
    assertEquals(sent.data, receivedMessage.data);
    assertEquals(sent.data2, receivedMessage.data2);
    assertEquals(sent.data3, receivedMessage.data3);
    
    assertEquals(false, queue.hasMessage());
  }
  
  @Test
  public void testPollByteArrayMessages() throws IOException, InterruptedException {
    ptp.reuseHiddenService();
    ptp2.reuseHiddenService();
    
    ptp2.enableMessageQueue();
    
    assertNotEquals(null, ptp2.getIdentifier());
    
    int sendCount = 3;
    
    for (int i = 0; i < sendCount; i++) {
      ptp.sendMessage(testData, ptp2.getIdentifier());
    }
    
    IMessageQueue<byte[]> receiveQueue = ptp2.getMessageQueue();

    long start = System.currentTimeMillis();

    while (!receiveQueue.hasMessage()
        && System.currentTimeMillis() - start < TestConstants.hiddenServiceSetupTimeout) {
      Thread.sleep(1000);
    }

    // Wait for messages to arrive
    TestHelper.sleep(TestConstants.listenerTimeout);
    
    int receiveCount = 0;
    
    while (receiveQueue.hasMessage()) {
      assertArrayEquals(testData, receiveQueue.pollMessage().getData());
      receiveCount++;
    }
    
    assertEquals(sendCount, receiveCount);
  }
  
  @Test (expected = IllegalArgumentException.class)
  public void testEnableQueueUnregisteredClass() throws IOException {    
    ptp.enableMessageQueue(TestMessage.class);
  }
  
  @Test
  public void testByteArrayMessageQueue() throws IOException, InterruptedException {
    ptp.reuseHiddenService();
    ptp2.reuseHiddenService();

    ptp.enableMessageQueue();
    ptp2.enableMessageQueue();

    assertNotEquals(null, ptp2.getIdentifier());

    byte[] message = "Test123".getBytes(Constants.charset);
    long sent = System.currentTimeMillis();
    ptp.sendMessage(message, ptp2.getIdentifier());
    
    IMessageQueue<byte[]> receiveQueue = ptp2.getMessageQueue();

    long start = System.currentTimeMillis();

    while (!receiveQueue.hasMessage()
        && System.currentTimeMillis() - start < TestConstants.hiddenServiceSetupTimeout) {
      Thread.sleep(1000);
    }
    
    assertTrue(receiveQueue.hasMessage());
    QueuedMessage<byte[]> received = receiveQueue.pollMessage();
    long now = System.currentTimeMillis();
    
    long receiveTime = received.getReceiveTime();
    assertTrue(sent < receiveTime && receiveTime < now);
    assertArrayEquals(message, received.getData());
    assertEquals(ptp.getIdentifier(), received.getSource());
    
    assertFalse(receiveQueue.hasMessage());
  }

}
