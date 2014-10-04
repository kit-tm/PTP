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


	/** TODO: write */
	private final Configuration config;
	/** TODO: write */
	private final Client client;
	/** TODO: write */
	private final TTLManager manager;


	// TODO: code comments
	// TODO: add logging


	/**
	 * TODO: write
	 *
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	public TorP2P() throws IllegalArgumentException, IOException {
		config = new Configuration("config/p2p.ini");
		client = new Client(config);
		manager = new TTLManager(client, 1 * 1000 /* TODO: config parameter for this */);
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
		if (connect == Client.ConnectResponse.SUCCESS)
			manager.put(identifier);

		Client.SendResponse response = client.send(identifier, message);

		if (response == Client.SendResponse.SUCCESS) {
			manager.set(identifier, 15 * 1000 /* TODO: config parameter for this */);
			return SendResponse.SUCCESS;
		}

		return SendResponse.FAIL;
	}

	/**
	 * TODO: write
	 *
	 * @see Client
	 */
	public void exit() {
		client.exit();
		manager.stop();
	}

}
