package thread;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import p2p.ReceiveListener;
import utility.RNG;


/**
 * This class offers JUnit testing for the SocketWaiter class.
 *
 * @author Simeon Andreev
 *
 */
public class SocketWaiterTest {


	/**
	 * Custom listener for the SocketWaiter.
	 *
	 * @author Simeon Andreev
	 *
	 */
	private static class Receiver implements ReceiveListener {

		/** An atomic boolean used to check whether a message was received yet. */
		public AtomicBoolean received = new AtomicBoolean(false);
		/** The received message. */
		public String message = null;


		/**
		 * @see ReceiveListener
		 */
		@Override
		public void receive(byte[] message) {
			this.message = new String(message);
			received.set(true);
		}

	}


	/**
	 * This class receives a single socket connection to the open server socket and later on sends a message on it.
	 *
	 * @author Simeon Andreev
	 *
	 */
	private class Sender implements Runnable {

		/** The socket connected to the open server socket. */
		private Socket socket = null;

		/**
		 * @see Runnable
		 */
		@Override
		public void run() {
			assert(server != null);

			// Wait for the socket connection on the open server socket.
			try {
				socket = server.accept();
			} catch (IOException e) {
				// If the socket connection failed the test is invalid.
				assertTrue(false);
			}
		}

		/**
		 * Sends a message via the socket connected to the open server socket.
		 *
		 * @param message The message to send.
		 * @throws IOException Throws an IOException if unable to send the message via the socket.
		 */
		public void Send(String message) throws IOException {
			// The socket should be connected prior sending.
			assertTrue(socket != null);

			// Send the message.
			socket.getOutputStream().write(message.getBytes());
		}

	}


	/** Delimiter used for the string to readable bytes conversion. */
	private static final String delimiter = "|";
	/** A string representation of the object null value. */
	private static final String nullbytes = "null";
	/** Minimum random message length used in the set test. */
	private static final int minimumMessageLength = 10;
	/** Maximum random message length used in the set test. */
	private static final int maximumMessageLength = 50;
	/** The local host address. */
	private static final String localhost = "localhost";

	/** The custom listener for the SocketWaiter. */
	private Receiver receiver = null;
	/** The object that will wait for a socket connection to the open server socket and will send a random message on it. */
	private Sender sender = null;
	/** The server socket to which a socket is connected. */
	private ServerSocket server = null;
	/** The thread waiting on the socket connection to the open server socket. */
	private Thread thread = null;
	/** The random message that is used used in the set test. */
	private String message = null;
	/** The socket connected to the open server socket, used by the SocketWaiter. */
	private Socket socket = null;
	/** The SocketWaiter for the test. */
	private SocketWaiter waiter = null;


	/**
	 * @throws IOException Throws an IOException if unable to open a server socket on any port, or if unable to open a socket connection to the server socket.
	 *
	 * @see JUnit
	 */
	@Before
	public void setUp() throws IOException {
		// Create the receiver and the sender.
		receiver = new Receiver();
		sender = new Sender();
		// Open the server socket on any port.
		server = new ServerSocket(0);
		// Accept a socket connection to the server socket.
		thread = new Thread(sender);
		thread.start();

		// Wait for the server socket thread to enter its execution loop.
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			// Sleeping was interrupted. Do nothing.
		}

		// Create the RNG.
		RNG random = new RNG();
		// Choose a random message with bounded length.
		message = random.string(minimumMessageLength, maximumMessageLength);

		// Open a socket connection to the server socket.
		socket = new Socket(localhost, server.getLocalPort());
		// Create a SocketWaiter with the open socket connection.
		waiter = new SocketWaiter(socket);
		// Start the SocketWaiter. Assumed to always work.
		waiter.start();
	}

	/**
	 * @throws IOException Propagates any IOException thrown when stopping the SocketWaiter, or throws an IOException when closing the open server socket.
	 *
	 * @see JUnit
	 */
	@After
	public void tearDown() throws IOException {
		// The thread accepting a socket connection to the server socket should have exited in the test set up method.
		assertTrue(!thread.isAlive());

		// Stop the SocketWaiter.
		waiter.stop();
		// Check if the socket connection to the server socket is still open and if so close it.
		if (!socket.isClosed())
			socket.close();

		// Close the server socket.
		server.close();
	}

	/**
	 * Test method for {@link thread.SocketWaiter#stop()}.
	 *
	 * Attempts to stop the SocketWaiter.
	 *
	 * Fails iff an exception is caught while stopping the SocketWaiter or the open socket connection to the SocketWaiter is not closed after the SocketWaiter is closed.
	 */
	@Test
	public void testStop() {
		// Attempt to stop the SocketWaiter.
		try {
			waiter.stop();
		} catch (IOException e) {
			fail("Caught IOException while stopping the SocketWaiter: " + e.getMessage());
		}
		// Check the socket connected to the SocketWaiter is now closed.
		if (!socket.isClosed())
			fail("Socket is still open after the SocketWaiter was stopped.");
	}

	/**
	 * Test method for {@link thread.Waiter#set(p2p.ReceiveListener)}.
	 *
	 * Sets the current listener of the SocketWaiter to a custom listener and sends a random message.
	 *
	 * Fails iff no message was received by the custom listener or the received message does not match the sent message.
	 */
	@Test
	public void testSet() {
		// Set the current listener of the SocketWaiter to the custom listener.
		waiter.set(receiver);

		// Attempt to send the message via a socket connection.
		try {
			sender.Send(message);
		} catch (IOException e) {
			// If the socket connection fails to send the message the test is invalid.
			assertTrue(false);
		}

		// Wait until the message is received, poll in 50 milliseconds interval for a second.
		final long start = System.currentTimeMillis();
		while (!receiver.received.get() && System.currentTimeMillis() - start < 1000) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// Sleeping was interrupted. Do nothing.
			}
		}

		// Check whether the received message equals the sent message, fail the test if not.
		if (!message.equals(receiver.message))
			fail("Received message does not equal sent message: " + toBytes(message) + " != " + toBytes(receiver.message));
	}


	/**
	 * Converts the characters of a string to bytes.
	 *
	 * @param message The string which characters should be converted.
	 * @return The bytes of the input strings characters.
	 */
	private String toBytes(String message) {
		// Check if the string is null, return null representation if so.
		if (message == null) return nullbytes;

		StringBuilder sb = new StringBuilder(delimiter);
		byte[] bytes = message.getBytes();

		// Add the message bytes to the stream.
		for (int i = 0; i < bytes.length; ++i) {
			sb.append(bytes[i]);
			sb.append(delimiter);
		}

		return sb.toString();
	}

}
