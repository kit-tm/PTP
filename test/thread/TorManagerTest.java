package thread;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;

import net.freehaven.tor.control.TorControlConnection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import tor.TorManager;
import utility.Constants;


/**
 * This class offers JUnit testing for the TorManager class.
 *
 * @author Simeon Andreev
 *
 */
public class TorManagerTest {

	/** The local host address. */
	private static final String localhost = "localhost";
	/** The destination address when testing the SOCKS proxy of the ready TorManagers Tor process. */
	private static final String destination = "google.com";
	/** The destination port when testing the SOCKS proxy of the ready TorManagers Tor process. */
	private static final int port = 80;

	/** The concurrent TorManager. */
	private TorManager concurrentManager = null;
	/** The ready TorManager. */
	private TorManager readyManager = null;
	/** The TorManager. */
	private TorManager manager = null;


	/**
	 * @throws IOException Propagates any IOException thrown by the TorManager creation.
	 *
	 * @see JUnit
	 */
	@Before
	public void setUp() throws IOException {
		// Set the logger format.
		System.setProperty(Constants.loggerconfig, "config/logger.ini");

		// Create the concurrent TorManager.
		concurrentManager = new TorManager();
		// Create the ready TorManager.
		readyManager = new TorManager();
		// Create the TorManager.
		manager = new TorManager();

		// Start the concurrent TorManager.
		concurrentManager.start();
		// Start the ready TorManager.
		readyManager.start();

		// Wait (no more than 3 minutes) until the ready TorManager is done with the Tor bootstrapping.
		final long start = System.currentTimeMillis();
		while ((!readyManager.ready() || !concurrentManager.ready()) && System.currentTimeMillis() - start < 180 * 1000) {
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
		// Stop the concurrent TorManager.
		concurrentManager.stop();
		// Stop the ready TorManager.
		readyManager.stop();
		// Stop the TorManager.
		manager.stop();
	}

	/**
	 * Test method for {@link tor.TorManager#stop()}.
	 *
	 * Checks whether the ready TorManager returns a non-running state after being stopped and deletes its working directory.
	 *
	 * Fails iff the ready TorManager returns a running state after being stopped, or if the ready TorManager working directory still exists.
	 */
	@Test
	public void testStop() {
		// Stop the concurrent TorManager.
		concurrentManager.stop();
		// Check whether the concurrent TorManager is still running.
		if (concurrentManager.running())
			fail("Concurrent TorManager is returning a running state after being stopped.");
		if (!concurrentManager.torrunning())
			fail("Concurrent TorManager is returning a stopped Tor process state after being stopped.");
		// Check whether the ready TorManager is still running.
		if (!readyManager.running())
			fail("Ready TorManager is returning a stopped state before being stopped.");
		if (!readyManager.torrunning())
			fail("Ready TorManager is returning a stopped Tor process state after being stopped.");
		// Stop the ready TorManager.
		readyManager.stop();
		// Check whether the ready TorManager is still running.
		if (readyManager.running())
			fail("Ready TorManager is returning a running state after being stopped.");
		if (readyManager.torrunning())
			fail("Ready TorManager is returning a running Tor process state after being stopped.");
	}

	/**
	 * Test method for {@link tor.TorManager#directory()}.
	 *
	 * Checks whether the working directory indicated by the ready TorManager exists.
	 *
	 * Fails iff the working directory indicated by the ready TorManager does not exist.
	 */
	@Test
	public void testDirectory() {
		if (!new File(readyManager.directory()).exists())
			fail("Ready TorManager directory does not exist.");
	}

	/**
	 * Test method for {@link tor.TorManager#ready()}.
	 *
	 * Checks whether the ready TorManager returns a ready state and if the TorManager returns a non-ready state.
	 *
	 * Fails iff the ready TorManager returns a non-ready states, or if the TorManager returns a ready state.
	 */
	@Test
	public void testReady() {
		if (!readyManager.ready())
			fail("Ready TorManager returns a non-ready state.");
		if (manager.ready())
			fail("TorManager returns a ready state.");
	}

	/**
	 * Test method for {@link tor.TorManager#controlport()}.
	 *
	 * Attempts to connect to the control port of the Tor process started by the ready TorManager.
	 *
	 * Fails iff the connection to the control port failed.
	 */
	@Test
	public void testControlport() {
		try {
			Socket s = new Socket(localhost, readyManager.controlport());
			TorControlConnection conn = TorControlConnection.getConnection(s);
			conn.authenticate(new byte[]{});
		} catch (IOException e) {
			fail("Caught an IOException while connecting to Tors control port: " + e.getMessage());
		}
	}

	/**
	 * Test method for {@link tor.TorManager#socksport()}.
	 *
	 * Attempts to use the SOCKS proxy of the Tor process (started by the ready TorManager) to connect to a destination address.
	 *
	 * Fails iff connecting to the destination fails.
	 */
	@Test
	public void testSocksport() {
		// Create a proxy by using the Tor SOCKS proxy.
		InetSocketAddress midpoint = new InetSocketAddress(localhost, readyManager.socksport());
		Proxy proxy = new Proxy(Proxy.Type.SOCKS, midpoint);
		Socket socket = new Socket(proxy);
		InetSocketAddress d = new InetSocketAddress(destination, port);

		try {
			socket.connect(d);
		} catch (IOException e) {
			fail("Using the proxy to connect to destination failed: " + destination + ":" + port);
		}

		try {
			socket.close();
		} catch (IOException e) {
			// The socket was closed. Do nothing.
		}
	}

	/**
	 * Test method for {@link thread.Suspendable#running()}.
	 *
	 * Checks whether the ready TorManager returns a running state and if the TorManager returns a non-running state.
	 *
	 * Fails iff the ready TorManager returns a non-running states, or if the TorManager returns a running state.
	 */
	@Test
	public void testRunning() {
		if (!readyManager.running())
			fail("Ready TorManager returns a non-running state.");
		if (manager.running())
			fail("TorManager returns a running state.");
	}

}
