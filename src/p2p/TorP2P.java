package p2p;

import java.io.IOException;

import thread.TTLManager;


/**
 * Wrapper class for the Tor2P2 raw API. Provides automatic socket and configuration management on top of the raw API.
 *
 * @author Simeon Andreev
 *
 */
public class TorP2P {


	/**
	 * A response enumeration for the send method.
	 * 		* SUCCESS indicates a successfully sent message.
	 * 		* CLOSED  indicates that the socket connection is not open.
	 * 		* FAIL    indicates a failure when sending the message.
	 */
	public enum SendResponse {
		SUCCESS,
		TIMEOUT,
		FAIL
	}


	/** The configuration of the client. */
	private final Configuration config;
	/** The raw API client. */
	private final Client client;
	/** The manager that closes sockets when their TTL expires. */
	private final TTLManager manager;


	/**
	 * Constructor method.
	 *
	 * @throws IllegalArgumentException
	 * @throws IOException
	 *
	 * @see Client
	 */
	public TorP2P() throws IllegalArgumentException, IOException {
		// Read the configuration.
		config = new Configuration("config/p2p.ini");
		// Create the client with the read configuration.
		client = new Client(config);
		// Create the manager with the given TTL.
		manager = new TTLManager(client, 1 * 1000 /* TODO: config parameter for this */);
	}


	/**
	 * Creates a fresh hidden service.
	 *
	 * @return The hidden service identifier of the created service.
	 * @throws IOException Propagates any IOException the API received while creating the hidden service.
	 *
	 * @see Client
	 */
	public String GetIdentifier() throws IOException {
		// Create a fresh hidden service identifier.
		return client.identifier(true);
	}

	/**
	 * Sends a message to a given hidden service identifier and port. Attempts to establish a connection within the given timeout.
	 *
	 * @param message The message that should be sent.
	 * @param identifier The destination hidden service identifier.
	 * @param port The destination port.
	 * @param timeout The timeout before exiting without a sent message.
	 * @return  TorP2P.SUCCESS    if the message was sent successfully.
	 * 			TorP2P.TIMEOUT    if the timeout was reached before a connection to the destination was established.
	 * 			TorP2P.FAIL       if the API received an exception while sending the message.
	 *
	 * @see Client
	 */
	public SendResponse SendMessage(String message, String identifier, int port, long timeout) {
		Client.ConnectResponse connect = Client.ConnectResponse.TIMEOUT;
		final long connectStart = System.currentTimeMillis();
		long remaining = timeout;

		// Attempt a connection to the given indentifier at an interval, until the given timeout is reached.
		while (connect == Client.ConnectResponse.TIMEOUT || connect == Client.ConnectResponse.FAIL) {
			try {
				// Set the remaining time until the timeout.
				remaining -= System.currentTimeMillis() - connectStart;
				// If the timeout is reached return with the corresnponding response.
				if (remaining < 0) return SendResponse.TIMEOUT;
				// Attempt a connection to the given identifier.
				connect = client.connect(identifier, port);
				// Sleep until the next attempt.
				Thread.sleep(Math.min(/* TODO: config parameter for this*/5 * 1000, remaining));
			} catch (InterruptedException e) {
				// Do nothing on interrupts.
			}
		}

		// If the connection to the destination hidden service was successful, add the identifier to the managed sockets.
		if (connect == Client.ConnectResponse.SUCCESS)
			manager.put(identifier);

		// Connection is successful
		Client.SendResponse response = client.send(identifier, message);

		// If the message was sent successfully, set the TTL of the socket opened for the identifier.
		if (response == Client.SendResponse.SUCCESS) {
			manager.set(identifier, 15 * 1000 /* TODO: config parameter for this */);
			return SendResponse.SUCCESS;
		}

		// Otherwise the API was unable to send the message in the given timeout.
		return SendResponse.FAIL;
	}

	/**
	 * Sets the current listener that should be notified on received messages.
	 *
	 * @param listener The new listener which should receive the notifications.
	 *
	 * @see Client
	 */
	public void SetListener(Listener listener) {
		// Propagate the input listener.
		client.listener(listener);
	}

	/**
	 * Returns the local port on which the local hidden service is available.
	 *
	 * @return The local port on which the local hidden service is available.
	 *
	 * @see Client
	 */
	public int GetLocalPort() {
		// Get the local port from the client.
		return client.localport();
	}

	/**
	 * Closes the local socket and any open connections. Stops the socket TTL manager.
	 *
	 * @see Client
	 */
	public void Exit() {
		// TODO: exit after timeout, save a state whether we exited and act accordingly in the other methods.
		// Close the client.
		client.exit();
		// Close the manager.
		manager.stop();
	}

}
