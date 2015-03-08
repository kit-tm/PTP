/**
 *
 */
package thread;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import api.Identifier;
import api.Message;
import api.MessageHandler;
import callback.ConnectionListener;
import callback.ReceiveListener;
import receive.MessageReceiver;
import utility.Constants;
import utility.RNG;


/**
 * This class offers JUnit testing for the MessageReceiver class.
 *
 * @author Simeon Andreev
 *
 */
public class MessageReceiverTest {

	/** The maximum number of threads to use for the message receiving. */
	private static final int threads = 1;
	/** The minimum message length of random messages. */
	private static final int minimumMessageLength = 10;
	/** The maximum message length of random messages. */
	private static final int maximumMessageLength = 20;
	/** The number of connections to use for the test. */
	private static final int c = 5;
	/** The number of random messages to send during the test. */
	private static final int n = 25;


	/**
	 * A custom listener to note the received messages.
	 *
	 * @author Simeon Andreev
	 *
	 */
	private static class Listener implements ReceiveListener {

		/** A hash map with which the received messages are counted. */
		public final HashMap<String, Integer> map = new HashMap<String, Integer>();
		/** A counter for received messages. */
		public final AtomicInteger counter = new AtomicInteger(0);


		/**
		 * @see ReceiveListener
		 */
		@Override
		public void receivedMessage(Message message) {
			String m = message.content;

			if (!map.containsKey(m)) map.put(m, 0);
			map.put(m, map.get(m) + 1);

			counter.incrementAndGet();
		}

	}

	/** A dummy listener which is notified on newly opened connections. */
	private final ConnectionListener dummyListener = new ConnectionListener() {

		/**
		 * @see ConnectionListener
		 */
		@Override
		public void ConnectionOpen(Identifier identifier, Socket socket) { /* Do nothing. */ }

	};


	/** The message receiver. */
	private MessageReceiver receiver;
	/** The listener to notify of received messages. */
	private Listener listener;
	/** A RNG to use for the test. */
	private RNG random;


	/**
	 * @see JUnit
	 */
	@Before
	public void setUp() {
		// Create and start the message receiver.
		try {
			receiver = new MessageReceiver(dummyListener, Constants.anyport, threads, 250);
		} catch (IOException e) {
			fail("Could not create message receiver object.");
		}
		receiver.start();
		// Create and set the listener.
		listener = new Listener();
		receiver.setListener(listener);
		// Create the RNG.
		random = new RNG();

		// Wait for the message receiver to enter its execution loop.
		final long start = System.currentTimeMillis();
		while (System.currentTimeMillis() - start < 1000) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// Was interrupted. Do nothing.
			}
		}
	}

	/**
	 *@see JUnit
	 */
	@After
	public void tearDown() {
		// Stop the message receiver.
		receiver.stop();
	}

	/**
	 * Tests the functionality of the MessageReceiver class. Send a number of random messages and check if all are received.
	 *
	 * Fails iff not all sent messages are received.
	 */
	@Test
	public void test() {
		// Generate random messages.
		String[] messages = new String[n];
		for (int i = 0; i < n; ++i)
			messages[i] = random.string(minimumMessageLength, maximumMessageLength);

		// Get the server socket port.
		final int port = receiver.getPort();
		// Open some connections to the server socket.
		Socket[] sockets = new Socket[c];
		for (int i = 0; i < c; ++i) {
			try {
				sockets[i] = new Socket(Constants.localhost, port);
			} catch (IOException e) {
				fail("Could not connect socket " + i + " to the local server socket.");
			}
		}

		// A reference hash map of the sent message counts.
		HashMap<String, Integer> map = new HashMap<String, Integer>();

		// Send the random messages.
		for (int i = 0; i < n; ++i) {
			// Add the message to the reference.
			if (!map.containsKey(messages[i])) map.put(messages[i], 0);
			map.put(messages[i], map.get(messages[i]) + 1);

			// Send the message via a random socket.
			final int index = random.integer(0, c - 1);
			try {
				Message m = MessageHandler.wrapMessage(new Message(messages[i], null));
				sockets[index].getOutputStream().write(m.content.getBytes());
			} catch (IOException e) {
				fail("Could not send the " + i + "th random message.");
			}
		}

		// Wait for the message receiver to receive the sent messages.
		final long start = System.currentTimeMillis();
		while (listener.counter.get() < n && System.currentTimeMillis() - start < 2500) {
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				// Was interrupted. Do nothing.
			}
		}

		for (int i = 0; i < n; ++i) {
			if (!listener.map.containsKey(messages[i]) || map.get(messages[i]).intValue() != listener.map.get(messages[i]))
				fail("Received message count does not match reference: " + messages[i]);
		}
	}

}
