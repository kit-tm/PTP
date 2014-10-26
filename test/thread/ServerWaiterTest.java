package thread;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import p2p.Listener;


/**
 * This class offers JUnit testing for the ServerWaiter class.
 *
 * @author Simeon Andreev
 *
 */
public class ServerWaiterTest {

	/** The port number specifiny that a server socket should be opened on any available port. */
	private static final int anyport = 0;
	/** The local host address. */
	private static final String localhost = "localhost";

	/** The socket that will be connected to the ServerSocket. */
	private Socket socket = null;
	/** The ServerWaiter for the test. */
	private ServerWaiter waiter = null;

	/**
	 * @throws IOException Propagates any IOException thrown when creating the ServerWaiter, or throws an IOException when opening a socket connection to the ServerWaiter.
	 *
	 * @see JUnit
	 */
	@Before
	public void setUp() throws IOException {
		// Create and start the ServerWaiter.
		waiter = new ServerWaiter(anyport);
		waiter.start();

		// Wait for the ServerWaiter thread to start waiting on connections.
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			// Sleeping was interrupted. Do nothing.
		}

		// Open a socket connection to the ServerWaiter.
		socket = new Socket(localhost, waiter.port());

		// Wait for the ServerWaiter thread to accept the connection.
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			// Sleeping was interrupted. Do nothing.
		}
	}

	/**
	 * @throws IOException Propagates any IOException thrown when stopping the ServerWaiter, or throws an IOException when closing the socket connection to the ServerWaiter.
	 *
	 * @see JUnit
	 */
	@After
	public void tearDown() throws IOException {
		socket.close();
		waiter.stop();
	}

	/**
	 * Test method for {@link thread.ServerWaiter#set(p2p.Listener)}.
	 *
	 * Attempts to send a message via the socket connected to the ServerWaiter and checks whether the message is propagated to a custom listener.
	 *
	 * Fails iff the custom listener does not receive the sent message, or if the received message is not equal to the sent message.
	 */
	@Test
	public void testSet() {
		// The message for the test.
		final String message = "Hello world.";
		// An atomic boolean used to test if the message is received yet.
		final AtomicBoolean received = new AtomicBoolean(false);

		// Set a custom listener for the ServerWaiter.
		waiter.set(new Listener() {

			@Override
			public void receive(byte[] bytes) {
				String m = new String(bytes);

				// Check if the received message equals the sent message.
				if (!message.equals(m))
					fail("Received message does not match the sent message: " + message + " != " + m);

				received.set(true);
			}

		});

		// Send the message via the socket connected to the ServerWaiter
		try {
			socket.getOutputStream().write(message.getBytes());
		} catch (IOException e) {
			// If the sending failed the test is invalid.
			assertTrue(false);
		}

		// Wait 1 second for the message to be received, poll at 50 milliseconds interval.
		final long start = System.currentTimeMillis();
		while (!received.get() && System.currentTimeMillis() - start < 1000) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// Sleeping was interrupted. Do nothing.
			}
		}

		if (!received.get())
			fail("Sent message was not received.");
	}

	/**
	 * Test method for {@link thread.ServerWaiter#stop()}.
	 *
	 * Attempts to stop the ServerWaiter and checks whether the socket connected to the ServerWaiter is closed.
	 *
	 * Fails iff the ServerSocket throws an exception during the stop call, or if the connected socket is still closed after the ServerSocket is stopped.
	 */
	@Test
	public void testStop() {
		// Attempt to stop the ServerWaiter.
		try {
			waiter.stop();
		} catch (IOException e) {
			fail("Caught an IOException while stopping server: " + e.getMessage());
		}

		// Check if the socket connected to the ServerSocket is still connected.
		try {
			if (socket.getInputStream().read() != -1)
				fail("Socket connected to the ServerWaiter not closed after stopping the ServerWaiter.");
		} catch (IOException e) {
			// If the check fails due to an IOException the test is invalid.
			assertTrue(false);
		}
	}

	/**
	 * Test method for {@link thread.ServerWaiter#port()}.
	 *
	 * Attempts to open a socket connection on the local port of the ServerWaiter.
	 *
	 * Fails iff the connection was unsuccessful.
	 */
	@Test
	public void testPort() {
		// Get the local port of the ServerWaiter.
		final int port = waiter.port();
		Socket socket = null;
		// Attempt to open the socket connection.
		try {
			socket = new Socket(localhost, port);
			if (!socket.isConnected())
				fail("Could not connect to the ServerWaiter.");
		} catch (IOException e) {
			fail("Caught an IOException while connecting to the ServerWaiter: " + e.getMessage());
		}
		// Close the socket connection in case it is open.
		try {
			if (socket != null)
				socket.close();
		} catch (IOException e) {
			// The socket was closed. Do nothing.
		}
	}

}
