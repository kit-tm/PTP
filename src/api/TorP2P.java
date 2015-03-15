package api;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import adapters.SendListenerAdapter;
import callback.DispatchListener;
import callback.ExpireListener;
import callback.ReceiveListener;
import callback.SendListener;
import connection.TTLManager;
import dispatch.MessageDispatcher;
import tor.TorManager;
import utility.Constants;


/**
 * Wrapper class for the Tor2P2 raw API. Provides the following on top of the raw API:
 * 		* automatic socket management
 * 		* sets configuration parameters
 * 		* uses a shared Tor instance
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
	private final Logger logger;
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
	/** A dummy sending listener to use when no listener is specified upon message sending. */
	private final SendListener dummyListener = new SendListenerAdapter();
	/** The identifier of the currently used hidden service. */
	private Identifier current = null;
	/** Indicates whether this API wrapper should reuse a hidden service. */
	private final boolean reuse;

	/**
	 * Constructor method. Creates an API wrapper which manages a Tor process.
	 *
	 * @throws IllegalArgumentException Propagates any IllegalArgumentException thrown by the reading of the configuration.
	 * @throws IOException Propagates any IOException thrown by the construction of the raw API, the configuration, or the Tor process manager.
	 *
	 * @see Client
	 * @see Configuration
	 * @see TorManager
	 */
	public TorP2P() throws IllegalArgumentException, IOException {
		this(null);
	}

	/**
	 * Constructor method. Creates an API wrapper which manages a Tor process.
	 *
	 * @param directory The name of the hidden service to reuse. May be null to indicate no specific reuse request.
	 * @throws IllegalArgumentException Propagates any IllegalArgumentException thrown by the reading of the configuration.
	 * @throws IOException Propagates any IOException thrown by the construction of the raw API, the configuration, or the Tor process manager.
	 *
	 * @see Client
	 * @see Configuration
	 * @see TorManager
	 */
	public TorP2P(String directory) throws IllegalArgumentException, IOException {
		// Read the configuration.
		config = new Configuration(Constants.configfile);
		// Create the logger after the configuration sets the logger properties file.
		logger = Logger.getLogger(Constants.torp2plogger);

		// Create the Tor process manager and start the Tor process.
		tor = new TorManager();
		// Start the Tor process.
		tor.start();

		// Did not receive a hidden service directory to reuse.
		reuse = directory != null;

		// Wait until the Tor bootstrapping is complete.
		long waited = 0;

		logger.log(Level.INFO, "Waiting for Tors bootstrapping to finish.");
		while (!tor.ready() && tor.running() && waited < config.getTorBootstrapTimeout()) {
			try {
				final long start = System.currentTimeMillis();
				Thread.sleep(250);
				waited += System.currentTimeMillis() - start;
			} catch (InterruptedException e) {
				// Waiting was interrupted. Do nothing.
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

		// Set the control ports.
		config.setTorConfiguration(tor.directory(), tor.controlport(), tor.socksport());
		// Create the client with the read configuration and set its receiving listener.
		client = new Client(config, directory);
		// Create and start the manager with the given TTL.
		manager = new TTLManager(getTTLManagerListener(), config.getTTLPoll());
		manager.start();
		// Create and start the message dispatcher.
		dispatcher = new MessageDispatcher(getMessageDispatcherListener(), config.getDispatcherThreadsNumber(), config.getSocketTimeout());
	}

	/**
	 * Constructor method. Creates an API wrapper which uses a Tor process running outside the API.
	 *
	 * @param workingDirectory The working directory of the Tor process.
	 * @param controlPort The control port of the Tor process.
	 * @param socksPort The SOCKS port of the Tor process.
	 * @throws IllegalArgumentException Propagates any IllegalArgumentException thrown by the reading of the configuration.
	 * @throws IOException Propagates any IOException thrown by the construction of the raw API, the configuration, or the Tor process manager.
	 *
	 * @see Client
	 * @see Configuration
	 */
	public TorP2P(String workingDirectory, int controlPort, int socksPort) throws IllegalArgumentException, IOException {
		this(workingDirectory, controlPort, socksPort, Constants.anyport, null);
	}

	/**
	 * Constructor method. Creates an API wrapper which uses a Tor process running outside the API.
	 *
	 * @param workingDirectory The working directory of the Tor process.
	 * @param controlPort The control port of the Tor process.
	 * @param socksPort The SOCKS port of the Tor process.
	 * @param socksPort The port of the local hidden service.
	 * @param directory The name of the hidden service to reuse. May be null to indicate no specific reuse request.
	 * @throws IllegalArgumentException Propagates any IllegalArgumentException thrown by the reading of the configuration.
	 * @throws IOException Propagates any IOException thrown by the construction of the raw API, the configuration, or the Tor process manager.
	 *
	 * @see Client
	 * @see Configuration
	 */
	public TorP2P(String workingDirectory, int controlPort, int socksPort, int localPort, String directory) throws IllegalArgumentException, IOException {
		// Read the configuration.
		config = new Configuration(workingDirectory + "/" + Constants.configfile);
		// Create the logger after the configuration sets the logger properties file.
		logger = Logger.getLogger(Constants.torp2plogger);

		// We will use an already running Tor instance, instead of managing one.
		tor = null;

		// Set the control ports.
		config.setTorConfiguration(workingDirectory, controlPort, socksPort);
		// Create the client with the read configuration and set its hidden service directory if given.
		reuse = directory != null;
		client = new Client(config, localPort, directory);
		// Create and start the manager with the given TTL.
		manager = new TTLManager(getTTLManagerListener(), config.getTTLPoll());
		manager.start();
		// Create and start the message dispatcher.
		dispatcher = new MessageDispatcher(getMessageDispatcherListener(), config.getDispatcherThreadsNumber(), config.getSocketTimeout());
	}


	/**
	 * Returns the currently used hidden service identifier.
	 *
	 * @return The hidden service identifier of the used/created hidden service.
	 *
	 * @see Client
	 */
	public Identifier getIdentifier() {
		return current;
	}

	/**
	 * Creates a hidden service, if possible reuses the hidden service indicated at API wrapper creation.
	 *
	 * @return The hidden service identifier of the created service.
	 * @throws IOException Propagates any IOException the API received while creating the hidden service.
	 *
	 * @see Client
	 */
	public void reuseHiddenService() throws IOException {
		if (current == null) current = new Identifier(client.identifier(!reuse));
	}

	/**
	 * Creates a fresh hidden service.
	 *
	 * @return The hidden service identifier of the created service.
	 * @throws IOException Propagates any IOException the API received while creating the hidden service.
	 *
	 * @see Client
	 */
	public void createHiddenService() throws IOException {
		// Create a fresh hidden service identifier.
		current = new Identifier(client.identifier(true));
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
	public void sendMessage(Message message, long timeout) {
		// Delegate with the dummy listener.
		sendMessage(message, timeout, dummyListener);
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
	public void sendMessage(Message message, long timeout, SendListener listener) {
		// Alter the content with the message handler and dispatch.
		dispatcher.enqueueMessage(message, timeout, listener);
	}

	/**
	 * Sets the current listener that should be notified on received messages.
	 *
	 * @param listener The new listener which should receive the notifications.
	 *
	 * @see Client
	 */
	public void setListener(ReceiveListener listener) {
		// Propagate the receive listener.
		client.listener(listener);
	}

	/**
	 * Returns the local port on which the local hidden service is available.
	 *
	 * @return The local port on which the local hidden service is available.
	 *
	 * @see Client
	 */
	public int getLocalPort() {
		// Get the local port from the client.
		return client.localPort();
	}

	/**
	 * Closes the local socket and any open connections. Stops the socket TTL manager and the Tor process manager.
	 *
	 * @see Client
	 */
	public void exit() {
		// Close the client.
		client.exit(!reuse);
		// Close the socket TTL manager.
		manager.stop();
		// Close the message dispatcher.
		dispatcher.stop();
		// Close the Tor process manager.
		if (tor != null) tor.stop();
	}


	/**
	 * Returns a manager listener that will close socket connections with expired TTL.
	 *
	 * @return The manager listener which closes sockets with no TTL left.
	 */
	private ExpireListener getTTLManagerListener() {
		return new ExpireListener() {

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
	 * Returns a message dispatch listener that will attempt to send the message to its destination.
	 *
	 * @return The manager listener which attempts sending.
	 */
	private DispatchListener getMessageDispatcherListener() {
		return new DispatchListener() {

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
			public boolean dispatch(Message message, SendListener listener, long timeout, long elapsed) {
				logger.log(Level.INFO, "Attempting to send message: " + "timeout = " + timeout + ", wait = " + elapsed + ", message = " + message.content + ", destination = " + message.identifier.getTorAddress());

				// If the timeout is reached return with the corresponding response.
				if (elapsed >= timeout) {
					logger.log(Level.INFO, "Timeout on message expired: " + message.content);
					listener.sendFail(message, SendListener.FailState.CONNECTION_TIMEOUT);
					return true;
				}

				// Attempt a connection to the given identifier.
				Client.ConnectResponse connect = client.connect(message.identifier.getTorAddress(), config.getSocketTimeout());
				// If the connection to the destination hidden service was successful, add the destination identifier to the managed sockets.
				if (connect == Client.ConnectResponse.SUCCESS) {
					manager.put(message.identifier);
				// Otherwise, if the connection was not successful indicate that sending should be retried.
				} else if (connect != Client.ConnectResponse.OPEN)
					return false;

				// Connection is successful, send the message.
				Client.SendResponse response = client.send(message.identifier.getTorAddress(), MessageHandler.wrapMessage(message).content);

				// If the message was sent successfully, set the TTL of the socket opened for the identifier.
				if (response == Client.SendResponse.SUCCESS) {
					manager.set(message.identifier, config.getSocketTTL());
					listener.sendSuccess(message);
					return true;
				}

				// Otherwise, indicate that the message was not sent successfully.
				listener.sendFail(message, SendListener.FailState.SEND_TIMEOUT);
				return true;
			}
		};
	}

}
