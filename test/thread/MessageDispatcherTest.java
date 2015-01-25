/**
 *
 */
package thread;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import utility.RNG;
import adapters.SendListenerAdapter;
import api.Identifier;
import api.Message;
import callback.DispatchListener;
import callback.SendListener;
import dispatch.MessageDispatcher;


/**
 * This class offers JUnit testing for the MessageDispatcher class.
 *
 * @author Simeon Andreev
 *
 */
public class MessageDispatcherTest {

	/** The minimum message length of the random messages. */
	private static final int minimumMessageLength = 10;
	/** The maximum message length of the random messages. */
	private static final int maximumMessageLength = 20;
	/** The number of test messages to dispatch. */
	private static final int n = 25;


	/**
	 * A dummy listener for the MessageDispatcher, used to test dispatching.
	 *
	 * @author Simeon Andreev
	 *
	 */
	private static class Client implements DispatchListener {

		/** An atomic counter indicating the number of dispatched messages. */
		public final AtomicInteger counter = new AtomicInteger(0);
		/** A set of dispatched message identifiers. */
		public final HashMap<String, Integer> map = new HashMap<String, Integer>();
		/** A RNG to sometimes not dispatch a message. */
		private final Random random = new Random();

		/**
		 * @see MessageDispatcher.Listener
		 */
		@Override
		public boolean dispatch(Message message, SendListener lisener, long timeout, long elapsed) {
			if (random.nextDouble() < 0.25)
				return false;

			if (!map.containsKey(message.content)) map.put(message.content, 0);
			map.put(message.content, map.get(message.content) + 1);

			counter.incrementAndGet();

			return true;
		}

	}


	/** Dummy destinations for the sending test. */
	private final String destinations[] = { "d1", "d2", "d3", "d4", "d5" };

	/** A dummy listener to receive dispatcher notifications. */
	private final SendListener listener = new SendListenerAdapter();

	/** The started message dispatcher. */
	private MessageDispatcher dispatcher;
	/** The dummy client. */
	private Client client;
	/** The RNG to use for the test. */
	private RNG random;


	/**
	 * @see JUnit
	 */
	@Before
	public void setUp() {
		client = new Client();
		dispatcher = new MessageDispatcher(client, 2);
		random = new RNG();

		// Wait for the dispatcher thread to enter its execution loop.
		final long start = System.currentTimeMillis();
		while (System.currentTimeMillis() - start < 1 * 1000) {
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				// Sleeping was interrupted. Do nothing.
			}
		}
	}

	/**
	 * @see JUnit
	 */
	@After
	public void tearDown() {
		dispatcher.stop();
	}

	/**
	 * Tests the functionality of the MessageDispatcher class.
	 * Dispatches a number of messages with random flags and checks if all messages are dispatched.
	 *
	 * Fails iff a message was not dispatched. Check is done based on message flags.
	 */
	@Test
	public void test() {
		// Create random messages and set reference values.
		String[] generated = new String[n];
		HashMap<String, Integer> map = new HashMap<String, Integer>();

		for (int i = 0; i < n; ++i) {
			generated[i] = random.string(minimumMessageLength, maximumMessageLength);

			if (!map.containsKey(generated[i])) map.put(generated[i], 0);
			map.put(generated[i], map.get(generated[i]) + 1);
		}

		// Sent messages with the random flags, check if they are all dispatched before a timeout (based on flags).
		for (int i = 0; i < n; ++i)
			dispatcher.enqueueMessage(new Message(generated[i], new Identifier(destinations[random.integer(0, destinations.length - 1)])), 5 * 1000, listener);

		// Wait for some time.
		final long start = System.currentTimeMillis();
		while (System.currentTimeMillis() - start < 3 * 1000 && client.counter.get() < n) {
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				// Sleeping was interrupted. Do nothing.
			}
		}

		// Check if all flags were dispatched.
		for (int i = 0; i < n; ++i) {
			if (!client.map.containsKey(generated[i]) || map.get(generated[i]).intValue() != client.map.get(generated[i]).intValue())
				fail("Message was not dispatched: " + generated[i]);
		}
	}

}
