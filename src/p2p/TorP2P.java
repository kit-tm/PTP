package p2p;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import thread.MessageDispatcher;
import thread.TTLManager;
import thread.TorManager;


/**
 * Wrapper class for the Tor2P2 raw API. Provides nn top of the raw API:
 * 		* automatic socket management
 * 		* sets configuration parameters
 * 		* starts a private Tor instance
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


	/** The logger for this class. */
	protected final Logger logger = Logger.getLogger(Constants.torp2plogger);
	/** The configuration of the client. */
	private final Configuration config;
	/** The Tor process manager. */
	private final TorManager tor;
	/** The raw API client. */
	private final Client client;
	/** The manager that closes sockets when their TTL expires. */
	private final TTLManager manager;
	/** The message dispatcher which sends the messages */
	private final MessageDispatcher dispatcher;


	/**
	 * Constructor method.
	 *
	 * @throws IllegalArgumentException
	 * @throws IOException Propagates an IOException thrown by the construction of the raw API, the configuraiton, or the Tor process manager.
	 *
	 * @see Client
	 * @see Configuration
	 * @see TorManager
	 */
	public TorP2P() throws IllegalArgumentException, IOException {
		// Create the Tor process manager and start the Tor process.
		tor = new TorManager();
		tor.start();

		// Wait until Tors bootstrapping is complete.
		// TODO: how to set a parameter for this? the configuration depends on the Tor process to have created the control port file, i.e. we must wait before creating the configuration
		final long timeout = 120000;
		final long poll = 2000;
		long waited = 0;

		logger.log(Level.INFO, "Waiting for Tors bootstrapping to finish.");
		while (!tor.ready() && tor.running() && waited < timeout) {
			try {
				final long start = System.currentTimeMillis();
				Thread.sleep(poll);
				waited += System.currentTimeMillis() - start;
			} catch (InterruptedException e) {
				logger.log(Level.INFO, "Waiting interrupted.");
			}
		}

		// Check if Tor is not running.
		if (!tor.running())
			throw new IllegalArgumentException("Starting Tor failed!");

		// Check if we reached the timeout without a finished boostrapping.
		if (!tor.ready()) {
			tor.killtor();
			throw new IllegalArgumentException("Tor bootstrapping timeout expired!");
		}

		// Read the configuration.
		config = new Configuration(Constants.configfile, tor.directory(), tor.controlport(), tor.socksport());
		// Create the client with the read configuration.
		client = new Client(config);
		// Create the manager with the given TTL.
		manager = new TTLManager(getTTLManagerListener(), config.getTTLPoll());
		// Create the message dispatcher.
		dispatcher = new MessageDispatcher(getMessageDispatcherListener(), config.getConnectionPoll());
	}


	/**
	 * Creates a fresh hidden service.
	 *
	 * @return The hidden service identifier of the created service.
	 * @throws IOException Propagates any IOException the API received while creating the hidden service.
	 *
	 * @see Client
	 */
	public Identifier GetIdentifier() throws IOException {
		// Create a fresh hidden service identifier.
		return new Identifier(client.identifier(true));
	}

	/**
	 * Sends a message to a given hidden service identifier and port.
	 * Success not guaranteed.
	 *
	 * @param message The message that should be sent.
	 * @param timeout The timeout before exiting without a sent message.
	 *
	 * @see Client
	 */
	public void SendMessage(Message message, long timeout) {
		dispatcher.dispatchMessage(message, timeout, new SendListener() {

			@Override
			public void connectionSuccess(Message message) {}

			@Override
			public void connectionTimeout(Message message) {}

			@Override
			public void sendSuccess(Message message) {}

			@Override
			public void sendFail(Message message) {}

		});
	}

	/**
	 * Sends a message to a given hidden service identifier and port and notify the given listener of sending events.
	 * Success not guaranteed, listener is notified on failure.
	 *
	 * @param message The message that should be sent.
	 * @param timeout The timeout before exiting without a sent message.
	 * @param listener The listener to notify of sending events.
	 *
	 * @see Client
	 */
	public void SendMessage(Message message, long timeout, SendListener listener) {
		dispatcher.dispatchMessage(message, timeout, listener);
	}

	/**
	 * Sets the current listener that should be notified on received messages.
	 *
	 * @param listener The new listener which should receive the notifications.
	 *
	 * @see Client
	 */
	public void SetListener(ReceiveListener listener) {
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
	 * Closes the local socket and any open connections. Stops the socket TTL manager and the Tor process manager.
	 *
	 * @see Client
	 */
	public void Exit() {
		// Close the client.
		client.exit();
		// Close the socket TTL manager.
		manager.stop();
		// Close the Tor process manager.
		tor.stop();
	}


	/**
	 * Returns a manager listener that will close socket connections with expired TTL.
	 *
	 * @return The manager listener which closes sockets with no TTL left.
	 */
	private TTLManager.Listener getTTLManagerListener() {
		return new TTLManager.Listener() {

			/**
			 * Set the listener to disconnect connections with expired TTL.
			 *
			 * @param identifier The identifier with the expired socket connection.
			 *
			 * @see TTLManager.Listener
			 */
			@Override
			public void expired(Identifier identifier) throws IOException {
				client.disconnect(identifier.getTorAddress());
			}

		};
	}

	/**
	 *
	 * @return
	 */
	private MessageDispatcher.Listener getMessageDispatcherListener() {
		return new MessageDispatcher.Listener() {

			/**
			 * Attempts to send a message via the raw API client. If the connection was not established and the timeout was not reached,
			 * will indicate that the message should be kept in the dispatcher queue.
			 *
			 * @param message The message to send.
			 * @param lisener The listener to be notified of sending events.
			 * @param timeout The timeout for the sending.
			 * @param elapsed The amount of time the message has waited so far.
			 * @return false if a further attempt to send the message should be done.
			 *
			 * @see MessageDispatcher.Listener
			 */
			@Override
			public boolean dispatch(Message message, SendListener listener, long elapsed, long timeout) {
				Client.ConnectResponse connect = Client.ConnectResponse.TIMEOUT;

				// If the timeout is reached return with the corresponding response.
				if (elapsed > timeout) {
					listener.connectionTimeout(message);
					return true;
				}

				// Attempt a connection to the given identifier.
				connect = client.connect(message.destination.getTorAddress(), config.getSocketTimeout());
				// If the connection to the destination hidden service was successful, add the destination identifier to the managed sockets.
				if (connect == Client.ConnectResponse.SUCCESS) {
					manager.put(message.destination);
					listener.connectionSuccess(message);
				// Otherwise, check if the connection was not successful.
				} else if (connect != Client.ConnectResponse.OPEN)
					return false;

				// Connection is successful, send the message.
				Client.SendResponse response = client.send(message.destination.getTorAddress(), message.content);

				// If the message was sent successfully, set the TTL of the socket opened for the identifier.
				if (response == Client.SendResponse.SUCCESS) {
					manager.set(message.destination, config.getSocketTTL());
					listener.sendSuccess(message);
					return true;
				}

				// Otherwise, indicate that the message was not sent successfully.
				listener.sendFail(message);
				return true;
			}
		};
	}

}
