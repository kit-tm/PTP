package p2p;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import api.Identifier;
import api.Message;
import api.TorP2P;
import callback.ReceiveListener;
import callback.SendListenerAdapter;
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
	private String message = null;


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
		message = random.string(minMessageLength, maxMessageLength);

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
		client1.Exit();
		client2.Exit();
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

		// Set the listener.
		client1.SetListener(new ReceiveListener() {

			@Override
			public void receivedMessage(byte[] bytes) {
				System.out.println("Received message: " + new String(bytes));
				if (!new String(bytes).equals(message))
					fail("Received message does not match sent message: " + message + " != " + new String(bytes));
				received.set(true);
			}

		});

		// Create the hidden service identifier.
		Identifier identifier = null;
		try {
			identifier = client1.GetIdentifier();
		} catch (IOException e) {
			fail("Caught an IOException while creating the hidden service identifier: " + e.getMessage());
		}

		// Send the message.
		final AtomicBoolean sendSuccess = new AtomicBoolean(false);
		final Message m = new Message(message, identifier);
		final long timeout = 180 * 1000;
		client1.SendMessage(m, timeout, new SendListenerAdapter() {

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

		Identifier i1 = null;
		Identifier i2 = null;
		try {
			i1 = client1.GetIdentifier();
			i2 = client2.GetIdentifier();
		} catch (IOException e) {
			fail("Caught an IOException while creating the identifiers: " + e.getMessage());
		}
		// The listeners need final values.
		final Identifier identifier1 = i1;
		final Identifier identifier2 = i2;

		// An atomic counter used to check the number of received messages.
		final AtomicInteger counter = new AtomicInteger(0);

		// Send a message to the second identifier and wait to ensure it is available.
		final AtomicBoolean sendSuccess1 = new AtomicBoolean(false);
		final AtomicBoolean sendSuccess2 = new AtomicBoolean(false);
		final Message m1 = new Message(message, identifier2);
		final Message m2 = new Message(message, identifier1);
		final long timeout = 180 * 1000;
		client1.SendMessage(m1, timeout, new SendListenerAdapter() {

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

		// Set the listeners.
		client1.SetListener(new ReceiveListener() {

			@Override
			public void receivedMessage(byte[] bytes) {
				counter.incrementAndGet();
				final String m = new String(bytes);
				if (!m.equals(message))
					fail("First API object received message does not match sent message: " + m + " != " + message);
				final Message msg = new Message(m, identifier2);
				client1.SendMessage(msg, 10 * 1000);
			}

		});
		client2.SetListener(new ReceiveListener() {

			@Override
			public void receivedMessage(byte[] bytes) {
				counter.incrementAndGet();
				final String m = new String(bytes);
				if (!m.equals(message))
					fail("Second API object received message does not match sent message: " + m + " != " + message);
				final Message msg = new Message(m, identifier1);
				client2.SendMessage(msg, 10 * 1000);
			}

		});

		// Send the initial ping-pong message.
		client2.SendMessage(m2, timeout, new SendListenerAdapter() {

			@Override
			public void sendSuccess(Message message) { sendSuccess2.set(true); }

		});
		client1.SendMessage(m1, timeout, new SendListenerAdapter() {

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
