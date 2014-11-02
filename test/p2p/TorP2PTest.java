package p2p;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
		fail("Not yet implemented");
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

		String i1 = null;
		String i2 = null;
		try {
			i1 = client1.GetIdentifier();
			i2 = client2.GetIdentifier();
		} catch (IOException e) {
			fail("Caught an IOException while creating the identifiers: " + e.getMessage());
		}
		// The listeners need final values.
		final String identifier1 = i1;
		final String identifier2 = i2;

		// An atomic counter used to check the number of received messages.
		final AtomicInteger counter = new AtomicInteger(0);

		// Set the listeners.
		client1.SetListener(new Listener() {

			@Override
			public void receive(byte[] bytes) {
				counter.incrementAndGet();
				final String m = new String(bytes);
				if (!m.equals(message))
					fail("First API object received message does not match sent message: " + m + " != " + message);
				client1.SendMessage(identifier2, m, 5 * 1000);
			}

		});
		client2.SetListener(new Listener() {

			@Override
			public void receive(byte[] bytes) {
				counter.incrementAndGet();
				final String m = new String(bytes);
				if (!m.equals(message))
					fail("First API object received message does not match sent message: " + m + " != " + message);
				client2.SendMessage(identifier1, m, 5 * 1000);
			}

		});

		// Send the initial ping-pong message.
		TorP2P.SendResponse response = client1.SendMessage(message, identifier2, 180 * 1000);
		if (response != TorP2P.SendResponse.SUCCESS)
			fail("Sending initial ping-pong message failed.");

		final long start = System.currentTimeMillis();
		while (counter.get() < max && System.currentTimeMillis() - start < 180 * 1000) {
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
