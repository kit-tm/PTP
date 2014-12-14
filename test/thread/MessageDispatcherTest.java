/**
 *
 */
package thread;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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


	/**
	 * A dummy listener for the MessageDispatcher, used to test dispatching.
	 *
	 * @author Simeon Andreev
	 *
	 */
	private static class Client extends DispatchListener {

		/** An atomic counter indicating the number of dispatched messages. */
		public final AtomicInteger counter = new AtomicInteger(0);
		/** A set of dispatched message identifiers. */
		public final HashSet<Long> dispatched = new HashSet<Long>();
		/** A RNG to sometimes not dispatch a message. */
		private final Random random = new Random();

		/**
		 * @see MessageDispatcher.Listener
		 */
		@Override
		public boolean dispatch(Message message, SendListener lisener, long timeout, long elapsed) {
			if (random.nextDouble() < 0.25)
				return false;

			dispatched.add(message.identifier);
			counter.incrementAndGet();

			return true;
		}

	}


	/** A dummy listener to receive dispatcher notifications. */
	private final SendListener listener = new SendListener() {};

	/** The started message dispatcher. */
	private MessageDispatcher dispatcher;
	/** The dummy client. */
	private Client client;
	/** A RNG for message identifiers. */
	private Random random;

	/**
	 * @see JUnit
	 */
	@Before
	public void setUp() {
		client = new Client();
		dispatcher = new MessageDispatcher(client, 2);
		random = new Random();

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
	 * Test method for {@link dispatch.MessageDispatcher#dispatchMessage(api.Message, long, callback.SendListener)}.
	 */
	@Test
	public void testDispatchMessage() {
		HashSet<Long> sent = new HashSet<Long>();

		// Create some random message identifiers.
		for (int i = 0; i < 50; ++i)
			sent.add(random.nextLong());

		// Sent messages with the random identifiers, check if they are all dispatched before a timeout.
		for (Long i : sent)
			dispatcher.enqueueMessage(new Message(i, "", new Identifier("")), 5 * 1000, listener);

		// Wait for some time.
		final long start = System.currentTimeMillis();
		while (System.currentTimeMillis() - start < 3 * 1000 && client.counter.get() < sent.size()) {
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				// Sleeping was interrupted. Do nothing.
			}
		}

		// Check if all identifiers were dispatched.
		for (Long i : sent) {
			if (!client.dispatched.contains(i))
				fail("Dispatched message identifier not contained in received identifiers: " + i);
		}
	}

}
