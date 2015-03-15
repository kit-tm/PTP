package p2p;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import adapters.SendListenerAdapter;
import api.Identifier;
import api.Message;
import api.TorP2P;
import callback.ReceiveListener;
import utility.RNG;


/**
 * This class offers JUnit testing for the TorP2P class.
 *
 * @author Simeon Andreev
 *
 */
public class TorP2PTest {

	/** The minimum length of the message used in the tests. */
	private static final int minMessageLength = 10;
	/** The maximum length of the message used in the tests. */
	private static final int maxMessageLength = 50;

	/** The first API wrapper object used in the self send test and in the ping-pong test. */
	private TorP2P client1 = null;
	/** The second API wrapper object used in the ping-pong test. */
	private TorP2P client2 = null;
	/** The message used in the tests. */
	private String testString = null;


	/**
	 * @throws IllegalArgumentException Propagates any IllegalArgumentException thrown by the API wrapper during construction.
	 * @throws IOException Propagates any IOException thrown by the API wrapper during construction.
	 *
	 * @see JUnit
	 */
	@Before
	public void setUp() throws IllegalArgumentException, IOException {
		// Create a RNG.
		RNG random = new RNG();

		// Generate a random message within the length bounds.
		testString = random.string(minMessageLength, maxMessageLength);

		// Create the API wrapper objects.
		client1 = new TorP2P();
		client2 = new TorP2P();
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
	 * Test for GetIdentifier()
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
	 * Test for fail handlers in SendListenerAdapter
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

		// a random valid address
		Identifier offlineId = new Identifier("bwehoflnshqul42e.onion");

		// helper class as we will be testing different addresses
		class TestSendFailHelper {

			private Message returnedMessage = null;

			public void run(Identifier id) {
				sendSuccess.set(false);
				sendFail.set(false);

				// Send a message.
				final Message m = new Message(testString, id);
				final long timeout = 20 * 1000;
				client1.sendMessage(m, timeout, new SendListenerAdapter() {

					@Override
					public void sendSuccess(Message message) { sendSuccess.set(true); }

					@Override
					public void sendFail(Message message, FailState state) {
						sendFail.set(true);
						returnedMessage = message;
					}
				});
				// Wait for the sending result.
				final long waitStart = System.currentTimeMillis();
				while ((System.currentTimeMillis() - waitStart <= timeout + (5 * 1000)) && !sendSuccess.get() && !sendFail.get()) {
					try {
						Thread.sleep(1 * 1000);
					} catch (InterruptedException e) {
						// Sleeping was interrupted. Do nothing.
					}
				}

				if (sendSuccess.get())
					fail("Received send success notification.");
				if (!sendFail.get())
					fail("No failure notification received");
				if (!returnedMessage.content.equals(testString))
					fail("Notifier message " + returnedMessage.content + " does not equal sent message " + testString);
			}
		}
		TestSendFailHelper helper = new TestSendFailHelper();
		helper.run(invalidId1);
		helper.run(invalidId2);
		helper.run(offlineId);
	}

	/**
	 * Test the API wrapper by sending a message to the local port, and then testing if the same message was received.
	 *
	 * Fails iff the sent message was not received within a time interval, or if the received message does not match the sent message.
	 */
	@Test
	public void testSelfSend() {
		// An atomic boolean used to check whether the sent message was received yet.
		final AtomicBoolean received = new AtomicBoolean(false);
		final AtomicBoolean matches = new AtomicBoolean(false);

		// Set the listener.
		client1.setListener(new ReceiveListener() {

			@Override
			public void receivedMessage(Message m) {
				System.out.println("Received message: " + m.content);
				if (!m.content.equals(testString))
					fail("Received message does not match sent message: " + testString + " != " + m.content);
				received.set(true);
				matches.set(m.content.equals(testString));
			}

		});

		// Create the hidden service identifier.
		Identifier identifier = null;
		try {
			client1.createHiddenService();
		} catch (IOException e) {
			fail("Caught an IOException while creating the hidden service identifier: " + e.getMessage());
		}

		identifier = client1.getIdentifier();

		// Send the message.
		final AtomicBoolean sendSuccess = new AtomicBoolean(false);
		final Message m = new Message(testString, identifier);
		final long timeout = 180 * 1000;
		client1.sendMessage(m, timeout, new SendListenerAdapter() {

			@Override
			public void sendSuccess(Message message) { sendSuccess.set(true); }

		});
		// Wait for the sending result.
		final long waitStart = System.currentTimeMillis();
		while (System.currentTimeMillis() - waitStart <= timeout + (5 * 1000) && !sendSuccess.get()) {
			try {
				Thread.sleep(1 * 1000);
			} catch (InterruptedException e) {
				// Sleeping was interrupted. Do nothing.
			}
		}
		if (!sendSuccess.get())
			fail("Sending the message via the client to the created identifier was not successful.");

		// Wait (no more than 3 minutes) until the message was received.
		final long start = System.currentTimeMillis();
		while (!received.get()) {
			try {
				Thread.sleep(5 * 1000);
				if (System.currentTimeMillis() - start > 180 * 1000)
					fail("Connecting to the created identifier took too long.");
			} catch (InterruptedException e) {
				// Waiting was interrupted. Do nothing.
			}
		}

		if (!received.get())
			fail("Message not received.");

		if (!matches.get())
			fail("Received message does not match sent message.");
	}

	/**
	 * Tests the API wrapper with a ping-pong between two API objects.
	 *
	 * Fails iff a received message does not match the first sent message, or if the number of received message does not reach the maximum number of messages to receive.
	 */
	@Test
	public void testPingPong() {
		// The maximum number of received messages during the ping-pong.
		final int max = 25;

        // Get the hidden service identifiers of the two API wrapper objects.
		try {
			client1.createHiddenService();
			client2.createHiddenService();
		} catch (IOException e) {
			fail("Caught an IOException while creating the identifiers: " + e.getMessage());
		}
		// The listeners need final values.
		final Identifier identifier1 = client1.getIdentifier();
		final Identifier identifier2 = client2.getIdentifier();

		// An atomic counter used to check the number of received messages.
		final AtomicInteger counter = new AtomicInteger(0);

		// Send a message to the second identifier and wait to ensure it is available.
		final AtomicBoolean sendSuccess1 = new AtomicBoolean(false);
		final AtomicBoolean sendSuccess2 = new AtomicBoolean(false);
		final Message m1 = new Message(testString, identifier2);
		final Message m2 = new Message(testString, identifier1);
		final long timeout = 180 * 1000;
		client1.sendMessage(m1, timeout, new SendListenerAdapter() {

			@Override
			public void sendSuccess(Message message) { sendSuccess1.set(true); }

		});
		// Wait for the sending result, to ensure second identifier is available.
		long waitStart = System.currentTimeMillis();
		while (System.currentTimeMillis() - waitStart <= timeout + (5 * 1000) && !sendSuccess1.get()) {
			try {
				Thread.sleep(1 * 1000);
			} catch (InterruptedException e) {
				// Sleeping was interrupted. Do nothing.
			}
		}
		if (!sendSuccess1.get())
			fail("Second hidden service identifier not available after timeout.");

		// Wait some extra time until the greeting message is propagated to the current listener.
		waitStart = System.currentTimeMillis();
		while (System.currentTimeMillis() - waitStart <= 2 * 1000) {
			try {
				Thread.sleep(1 * 1000);
			} catch (InterruptedException e) {
				// Sleeping was interrupted. Do nothing.
			}
		}

		// Set the listeners.
		client1.setListener(new ReceiveListener() {

			@Override
			public void receivedMessage(Message m) {
				counter.incrementAndGet();
				if (!m.content.equals(testString))
					fail("First API object received message does not match sent message: " + m.content + " != " + testString);
				final Message msg = new Message(m.content, identifier2);
				client1.sendMessage(msg, 10 * 1000);
			}

		});
		client2.setListener(new ReceiveListener() {

			@Override
			public void receivedMessage(Message m) {
				counter.incrementAndGet();
				if (!m.content.equals(testString))
					fail("Second API object received message does not match sent message: " + m.content + " != " + testString);
				final Message msg = new Message(m.content, identifier1);
				client2.sendMessage(msg, 10 * 1000);
			}

		});

		// Send the initial ping-pong message.
		client2.sendMessage(m2, timeout, new SendListenerAdapter() {

			@Override
			public void sendSuccess(Message message) { sendSuccess2.set(true); }

		});
		client1.sendMessage(m1, timeout, new SendListenerAdapter() {

			@Override
			public void sendSuccess(Message message) { sendSuccess1.set(true); }

		});

		// Wait for the sending result, to ensure first identifier is available.
		waitStart = System.currentTimeMillis();
		while (System.currentTimeMillis() - waitStart <= timeout + (5 * 1000) && !sendSuccess2.get()) {
			try {
				Thread.sleep(1 * 1000);
			} catch (InterruptedException e) {
				// Sleeping was interrupted. Do nothing.
			}
		}
		if (!sendSuccess2.get())
			fail("Sending initial ping-pong message failed.");

		// Wait for all ping-pong messages to arrive.
		final long start = System.currentTimeMillis();
		while (counter.get() < max && System.currentTimeMillis() - start < 300 * 1000) {
			try {
				Thread.sleep(5 * 1000);
			} catch (InterruptedException e) {
				// Waiting was interrupted. Do nothing.
			}
		}

		if (counter.get() < max)
			fail("Maximum number of received messages not reached.");
	}

}
