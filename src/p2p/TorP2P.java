package p2p;

import java.io.IOException;


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


	/** TODO: write */
	private final Configuration config;
	/** TODO: write */
	private final Client client;
	// TODO: socket closing thread if no send in a timeout value

	/**
	 * TODO: write
	 *
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	public TorP2P() throws IllegalArgumentException, IOException {
		config = new Configuration("config/p2p.ini");
		client = new Client(config);
	}


	/**
	 * TODO: write
	 *
	 * @param fresh
	 * @return
	 * @throws IOException
	 *
	 * @see Client
	 */
	public String identifier(boolean fresh) throws IOException {
		return client.identifier(fresh);
	}

	/**
	 * TODO: write
	 *
	 * @param message
	 * @param identifier
	 * @param port
	 * @param timeout
	 * @return
	 *
	 * @see Client
	 */
	public SendResponse send(String message, String identifier, int port, long timeout) {
		// TODO: write comments
		Client.ConnectResponse connect = Client.ConnectResponse.TIMEOUT;
		final long connectStart = System.currentTimeMillis();
		long remaining = timeout;
		while (connect == Client.ConnectResponse.TIMEOUT || connect == Client.ConnectResponse.FAIL) {
			try {
				remaining -= System.currentTimeMillis() - connectStart;
				if (remaining < 0) return SendResponse.TIMEOUT;
				connect = client.connect(identifier, port);
				Thread.sleep(Math.min(/* TODO: config parameter for this*/5 * 1000, remaining));
			} catch (InterruptedException e) {
				// Do nothing on interrupts.
			}
		}
		if (connect == Client.ConnectResponse.SUCCESS) {
			// TODO: add closing task to socket manager thread for the newly opened socket
		}

		Client.SendResponse response = client.send(identifier, message);

		if (response == Client.SendResponse.SUCCESS) return SendResponse.SUCCESS;

		return SendResponse.FAIL;
	}

}
