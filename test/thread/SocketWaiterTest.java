package thread;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import p2p.Listener;


/**
 *
 *
 * @author Simeon Andreev
 *
 */
public class SocketWaiterTest {


	/**
	 *
	 *
	 * @author Simeon Andreev
	 *
	 */
	private static class Receiver implements Listener {

		public AtomicBoolean received = new AtomicBoolean(false);
		public String message = null;


		@Override
		public void receive(byte[] message) {
			this.message = new String(message);
			received.set(true);
		}

	}


	/**
	 *
	 *
	 * @author Simeon Andreev
	 *
	 */
	private class Sender implements Runnable {

		private Socket socket = null;

		@Override
		public void run() {
			assert(server != null);

			try {
				socket = server.accept();
			} catch (IOException e) {
				assertTrue(false);
			}
		}

		public void Send(String message) throws IOException {
			assertTrue(socket != null);

			socket.getOutputStream().write(message.getBytes());
		}

	}


	// TODO: comments, java-doc

	private static final String delimiter = "|";
	private static final String nullbytes = "null";
	private static final int minimumMessageSize = 10;
	private static final int maximumMessageSize = 50;

	private final Receiver receiver = new Receiver();
	private final Sender sender = new Sender();
	private ServerSocket server = null;
	private Thread thread = null;
	private Random random = null;
	private Socket socket = null;
	private SocketWaiter waiter = null;

	/**
	 * @throws IOException
	 */
	@Before
	public void setUp() throws IOException {
		server = new ServerSocket(0);
		thread = new Thread(sender);
		thread.start();
		random = new Random();
		socket = new Socket("localhost", server.getLocalPort());
		waiter = new SocketWaiter(socket);
		// start() is assumed to always work.
		waiter.start();
	}

	/**
	 * @throws IOException
	 */
	@After
	public void tearDown() throws IOException {
		assertTrue(!thread.isAlive());

		waiter.stop();
		if (!socket.isClosed())
			socket.close();
		server.close();
	}

	/**
	 * Test method for {@link thread.SocketWaiter#stop()}.
	 */
	@Test
	public void testStop() {
		try {
			waiter.stop();
		} catch (IOException e) {
			fail("Caught IOException: " + e.getMessage());
		}
		if (!socket.isClosed())
			fail("Socket is still open after the waiter was stopped.");
	}

	/**
	 * Test method for {@link thread.Waiter#set(p2p.Listener)}.
	 */
	@Test
	public void testSet() {
		final int size = minimumMessageSize + random.nextInt(maximumMessageSize - minimumMessageSize + 1);
		byte[] buffer = new byte[size];
		random.nextBytes(buffer);
		final String message = new String(buffer);
		waiter.set(receiver);

		try {
			sender.Send(message);
		} catch (IOException e) {
			assertTrue(false);
		}

		final long start = System.currentTimeMillis();
		while (!receiver.received.get() && System.currentTimeMillis() - start < 1000) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {

			}
		}

		if (!message.equals(receiver.message))
			fail("Received message does not equal sent message: " + toBytes(message) + " != " + toBytes(receiver.message));
	}


	private String toBytes(String message) {
		if (message == null) return nullbytes;

		StringBuilder sb = new StringBuilder(delimiter);
		byte[] bytes = message.getBytes();

		for (int i = 0; i < bytes.length; ++i) {
			sb.append(bytes[i]);
			sb.append(delimiter);
		}

		return sb.toString();
	}

}
