package p2p;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import thread.TorManager;
import utility.RNG;


/**
 *
 *
 * @author Simeon Andreev
 *
 */
public class ClientTest {

	/** The minimum length of the message used in the tests. */
	private static final int minMessageLength = 10;
	/** The maximum length of the message used in the tests. */
	private static final int maxMessageLength = 50;


	/** The TorManager. */
	private TorManager manager = null;
	/** The message used in the tests. */
	private String message = null;
	/** The configuration used by the API. */
	Configuration configuration = null;
	/** The API object used in the self send test. */
	private Client client = null;


	/**
	 * @throws IOException Propagates any IOException thrown by the TorManager creation.
	 *
	 * @see JUnit
	 */
	@Before
	public void setUp() throws IOException {
		// Create a RNG.
		RNG random = new RNG();
		// Create a random message within the length bounds.
		message = random.string(minMessageLength, maxMessageLength);

		// Create the TorManager.
		manager = new TorManager();
		// Start the TorManager.
		manager.start();

		// Read the configuration.
		try {
			configuration = new Configuration(Constants.configfile, Paths.get(""), manager.controlport(), manager.socksport());
		} catch (IllegalArgumentException e) {
			// If the configuration fail is broken, the test is invalid.
			assertTrue(false);
		} catch (IOException e) {
			// If a failure occured while reading the configuration fail, the test is invalid.
			assertTrue(false);
		}

		// Wait (no more than 3 minutes) until the TorManager is done with the Tor bootstrapping.
		final long start = System.currentTimeMillis();
		while (!manager.ready() && System.currentTimeMillis() - start < 180 * 1000) {
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
		// Stop the TorManager.
		manager.stop();
		// Clean up the API used in the self send test.
		if (client != null) client.exit();
	}

	/**
	 * Test the raw API by sending a message to the local port, and then testing if the same message was received.
	 *
	 * Fails iff the sent message was not received within a time interval, or if the received message does not match the sent message.
	 */
	@Test
	public void testSelfSend() {
		// An atomic boolean used to check whether the sent message was received yet.
		final AtomicBoolean received = new AtomicBoolean(false);

		// Create the API object.
		try {
			client = new Client(configuration);
		} catch (IOException e) {
			fail("Caught an IOException while creating the API object: " + e.getMessage());
		}

		// Set the listener.
		client.listener(new Listener() {

			@Override
			public void receive(byte[] bytes) {
				System.out.println("Received message: " + new String(bytes));
				if (!new String(bytes).equals(message))
					fail("Received message does not match sent message: " + message + " != " + new String(bytes));
				received.set(true);
			}

		});

		// Create the hidden service identifier.
		String identifier = null;
		try {
			identifier = client.identifier(true);
		} catch (IOException e) {
			fail("Caught an IOException while creating the hidden service identifier: " + e.getMessage());
		}

		// Wait (no more than 3 minutes) until the API connects to the created identifier.
		long start = System.currentTimeMillis();
		Client.ConnectResponse connect = Client.ConnectResponse.TIMEOUT;
		while (connect == Client.ConnectResponse.TIMEOUT || connect == Client.ConnectResponse.FAIL) {
			try {
				connect = client.connect(identifier);
				Thread.sleep(5 * 1000);
				if (System.currentTimeMillis() - start > 180 * 1000)
					fail("Connecting to the created identifier took too long.");
			} catch (InterruptedException e) {
				// Waiting was interrupted. Do nothing.
			}
		}

		// Send the message.
		Client.SendResponse sendResponse = client.send(identifier, message);
		if (sendResponse != Client.SendResponse.SUCCESS)
			fail("Sending the message via the client to the created identifier was not successful.");

		// Wait (no more than 3 minutes) until the message was received.
		start = System.currentTimeMillis();
		while (!received.get()) {
			try {
				Thread.sleep(5 * 1000);
				if (System.currentTimeMillis() - start > 180 * 1000)
					fail("Connecting to the created identifier took too long.");
			} catch (InterruptedException e) {
				// Waiting was interrupted. Do nothing.
			}
		}

		// Disconnect the API from the created identifier.
		Client.DisconnectResponse disconnectResponse = client.disconnect(identifier);
		if (disconnectResponse != Client.DisconnectResponse.SUCCESS)
			fail("Disconncting the client from the created identifier was not successful.");
	}

	/**
	 * TODO: write
	 */
	@Test
	public void testPingPong() {
		fail("Not yet implemented");
	}

}
