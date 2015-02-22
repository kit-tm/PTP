package api;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import receive.MessageReceiver;
import utility.Constants;
import callback.ConnectionListener;
import callback.ReceiveListener;
import net.freehaven.tor.control.TorControlConnection;
import network.SOCKS;


/**
 * The raw Tor2P2 API, which requires a running Tor instance. It provides:
 * 		* create a local hidden service identifier (opens a local server)
 * 		* close any connections to the local identifier and close the local server
 * 		* open a socket connection to a given identifier
 * 		* close the socket connection to a given identifier
 * 		* send message to a given identifier
 *
 * @author Simeon Andreev
 *
 */
public class Client {


	/**
	 * A response enumeration for the exit method.
	 * 		* SUCCESS indicates a successfully closed server socket.
	 * 		* FAIL    indicates a failure when closing the server socket.
	 */
	public enum ExitResponse {
		SUCCESS,
		FAIL
	}

	/**
	 * A response enumeration for the connect method.
	 * 		* SUCCESS indicates a successfully opened socket connection.
	 * 		* TIMEOUT indicates a reached timeout upon opening the socket connection.
	 * 		* OPEN    indicates that a socket connection is already open.
	 * 		* FAIL    indicates a failure when opening the socket connection.
	 */
	public enum ConnectResponse {
		SUCCESS,
		TIMEOUT,
		OPEN,
		FAIL
	}

	/**
	 * A response enumeration for the disconnect method.
	 * 		* SUCCESS indicates a successfully closed socket connection.
	 * 		* CLOSED  indicates that the socket connection is not open.
	 * 		* FAIL    indicates a failure when closing the socket connection.
	 */
	public enum DisconnectResponse {
		SUCCESS,
		CLOSED,
		FAIL
	}

	/**
	 * A response enumeration for the send method.
	 * 		* SUCCESS indicates a successfully sent message.
	 * 		* CLOSED  indicates that the socket connection is not open.
	 * 		* FAIL    indicates a failure when sending the message.
	 */
	public enum SendResponse {
		SUCCESS,
		CLOSED,
		FAIL
	}


	/** The logger for this class. */
	private Logger logger = Logger.getLogger(Constants.clientlogger);

	/** The parameters of this client. */
	private final Configuration configuration;
	/** The raw API lock file. */
	private final File lockFile;
	/** The server socket receiving messages. */
	private final MessageReceiver receiver;
	/** The open socket connections to Tor hidden services. */
	private final ConcurrentHashMap<String, Socket> sockets = new ConcurrentHashMap<String, Socket>();
	/** The current hidden service identifier. */
	private String identifier = null;
	/** The hidden service sub-directory of this client. */
	private String directory = null;


	/**
	 * Constructor method.
	 *
	 * @param configuration The parameters of this client.
	 * @throws IOException Throws an IOException if unable to open a server socket on any port.
	 *
	 * @see Configuration
	 */
	public Client(Configuration configuration) throws IOException { this(configuration, null); }


	/**
	 * Constructor method.
	 *
	 * @param configuration The parameters of this client.
	 * @param directory The name of the hidden service directory this client will use. If null, the client will use the server socket port for the name.
	 * @throws IOException Throws an IOException if unable to open a server socket on any port.
	 *
	 * @see Configuration
	 */
	public Client(Configuration configuration, String directory) throws IOException {
		this.configuration = configuration;
		// Tell the JVM we want any available port.
		receiver = new MessageReceiver(getConnectionListener(), Constants.anyport, configuration.getReceiverThreadsNumber(), configuration.getSocketReceivePoll());
		receiver.start();

		// Check if the hidden service directory exists, if not create it.
		File hiddenServiceDirectory = new File(configuration.getHiddenServiceDirectory());
		if (!hiddenServiceDirectory.exists() && !hiddenServiceDirectory.mkdirs())
			throw new IOException("Could not create hidden service directory!");

		// Check if the lock file exists, if not create it.
		lockFile = new File(configuration.getWorkingDirectory() + File.separator + Constants.rawapilockfile);
		if (!lockFile.exists() && !lockFile.createNewFile())
			throw new IOException("Could not create raw API lock file!");

		identifier = configuration.getDefaultIdentifier();

		this.directory = directory != null ? directory : Constants.hiddenserviceprefix + receiver.getPort();
		logger.log(Level.INFO, "Client object created.");
	}


	/**
	 * Sets the current listener that should be notified of messages received by a socket connection.
	 *
	 * @param listener The new listener.
	 *
	 * @see Waiter
	 */
	public void listener(ReceiveListener listener) {
		receiver.setListener(listener);
	}

	/**
	 * Closes the server socket and any open receiving socket connections. Will not close connections open for sending.
	 *
	 * @param clean If true, will delete the current hidden service directory.
	 * @return  ExitResponse.FAIL    if an IOException occurred while closing the server socket, or when deleting the hidden service directory.
	 * 			ExitResponse.SUCCESS if the server socket was closed.
	 */
	public ExitResponse exit(boolean clean) {
		logger.log(Level.INFO, "Client exiting.");

		// Stop the message receiver and all threads waiting on open socket connections.
		logger.log(Level.INFO, "Stopping message receiver.");
		receiver.stop();

		if (clean) {
			try {
				// Delete the hidden service directory.
				deleteHiddenService();
			} catch (IOException e) {
				logger.log(Level.WARNING, "Received IOException while deleting the hidden service directory: " + e.getMessage());
				return ExitResponse.FAIL;
			}
		}

		logger.log(Level.INFO, "Stopped server waiter and deleted hidden service directory.");
		return ExitResponse.SUCCESS;
	}


	/**
	 * Returns the local Tor hidden service identifier. Will create a hidden service if none is present by using JTorCtl.
	 *
	 * @param fresh If true, will always generate a new identifier, even if one is already present.
	 * @return The hidden service identifier.
	 * @throws IOException Throws an exception when unable to read the Tor hostname file, or when unable to connect to Tors control socket.
	 */
	public String identifier(boolean fresh) throws IOException {
		logger.log(Level.INFO, (fresh ? "Fetching a fresh" : "Attempting to reuse (if present) the") + " identifier.");

		RandomAccessFile raf = null;
		FileChannel channel = null;
		FileLock lock = null;

		try {
			// Block until a lock on the raw API lock file is available.
			logger.log(Level.INFO, "Client acquiring lock on raw API lock file.");
			raf = new RandomAccessFile(lockFile, Constants.readwriterights);
			channel = raf.getChannel();
			lock = channel.lock();

			logger.log(Level.INFO, "Client acquired the lock on raw API lock file.");

			File dir = new File(configuration.getHiddenServiceDirectory() + File.separator + directory);
			final boolean create = !dir.exists();

			// If a fresh identifier is requested, delete the current hidden service directory.
			if (fresh && !create)
				deleteHiddenService();

			if (!dir.exists() && !dir.mkdir())
				throw new IOException("Unable to create the hidden service directory!");

			// If the Tor hostname file does not exist in the Tor hidden service directory, create a hidden service with JTorCtl.
			if (fresh || create) {
				logger.log(Level.INFO, "Creating hidden service.");
				createHiddenService();
			}

			File hostname = new File(configuration.getHiddenServiceDirectory() + File.separator + directory + File.separator + Constants.hostname);

			// Read the content of the Tor hidden service hostname file.
			identifier = readIdentifier(hostname);
			logger.log(Level.INFO, "Fetched hidden service identifier: " + identifier);

			return identifier;
		} finally {
			// Release the lock, if acquired.
			logger.log(Level.INFO, "Client releasing the lock on the raw API lock file.");

			if (lock != null) {
				lock.release();
				// Close the lock file.
				channel.close();
				raf.close();
			}
		}
	}



	/**
	 * Sends a message to an open socket at the specified Tor hidden service identifier.
	 *
	 * @param destination The  identifier of the destination Tor hidden service.
	 * @param message The message to be sent.
	 * @return  ConnectResponse.CLOSED  if a socket connection is not open for the identifier.
	 * 			ConnectResponse.FAIL    if an IOException occured while sending the message to the identifier.
	 * 			ConnectResponse.SUCCESS if the message was sent to the identifier.
	 */
	public SendResponse send(String destination, String message) {
		logger.log(Level.INFO, "Sending a message to identifier " + destination + " on port " + configuration.getHiddenServicePort() + ".");
		// Check if a socket is open for the specified identifier.
		if (!sockets.containsKey(destination)) {
			logger.log(Level.INFO, "No socket is open for identifier: " + destination);
			return SendResponse.CLOSED;
		}

		// Get the socket for the specified identifier.
		logger.log(Level.INFO, "Getting socket from map.");
		Socket socket = sockets.get(destination);

		try {
			// Send the message and close the socket connection.
			logger.log(Level.INFO, "Sending message: " + message);
			socket.getOutputStream().write(message.getBytes());
		} catch (IOException e) {
			// IOException when getting socket output stream or sending bytes via the stream.
			logger.log(Level.WARNING, "Received an IOException while sending a message to identifier = " + destination + ": " + e.getMessage());
			return SendResponse.FAIL;
		}

		logger.log(Level.INFO, "Sending done.");
		return SendResponse.SUCCESS;
	}

	/**
	 * Opens a socket connection to the specified Tor hidden service.
	 *
	 * @param identifier The Tor hidden service identifier of the destination.
	 * @param socketTimeout The socket connect timeout.
	 * @param port The port number of the destination Tor hidden service.
	 * @return  ConnectResponse.OPEN    if a socket connection is already open for the identifier.
	 * 			ConnectResponse.FAIL    if an IOException occured while opening the socket connection for the identifier.
	 * 			ConnectResponse.SUCCESS if a socket connection is now open for the identifier.
	 * 			ConnectResponse.TIMEOUT if the socket connection timeout was reached upon opening the connection to the identifier.
	 */
	public ConnectResponse connect(String destination, int socketTimeout) {
		logger.log(Level.INFO, "Opening a socket for identifier " + destination + " on port " + configuration.getHiddenServicePort() + ".");

		// Check if a socket is already open to the given identifier.
		if (sockets.containsKey(destination)) {
			logger.log(Level.INFO, "A socket is already open for the given identifier.");
			return ConnectResponse.OPEN;
		}

		ConnectResponse response = ConnectResponse.SUCCESS;

		try {
			// Open a socket implementing the SOCKS4a protocol.
			logger.log(Level.INFO, "Opening socket using the Tor SOCKS proxy, timeout: " + socketTimeout);
			Socket socket = SOCKS.socks4aSocketConnection(destination, configuration.getHiddenServicePort(), Constants.localhost, configuration.getTorSOCKSProxyPort(), socketTimeout);
			logger.log(Level.INFO, "Adding socket to open sockets.");
			sockets.put(destination, socket);
			logger.log(Level.INFO, "Opened socket for identifier: " + destination);
			// Send the current identifier as the first message.
			send(destination, MessageHandler.wrapRaw(identifier, Constants.messageoriginflag));
			// Add the new connection to the message receiver.
			receiver.addConnection(destination, socket);
		} catch (SocketTimeoutException e) {
			// Socket connection timeout reached.
			logger.log(Level.WARNING, "Timeout reached for connection to identifier: " + destination);
			response = ConnectResponse.TIMEOUT;
		} catch (IOException e) {
			logger.log(Level.WARNING, "Received an IOException while connecting the socket to identifier = " + destination + ": " + e.getMessage());
			response = ConnectResponse.FAIL;
		}

		return response;
	}

	/**
	 * Closes the socket connection for the specified Tor hidden service identifier.
	 *
	 * @param identifier The identifier of the Tor hidden service.
	 * @return  DisconnectResponse.CLOSED  if no socket connection for the identifier.
	 * 			DisconnectResponse.FAIL    if an IOException occured while closing the socket connection for the identifier.
	 * 			DisconnectResponse.SUCCESS if the socket connection for the identifier was closed.
	 */
	public DisconnectResponse disconnect(String identifier) {
		logger.log(Level.INFO, "Closing socket for identifier: " + identifier);

		// Check if the socket is open.
		if (!sockets.containsKey(identifier)) {
			logger.log(Level.INFO, "Socket not open.");
			return DisconnectResponse.CLOSED;
		}

		try {
			// Get the socket from the map.
			Socket socket = sockets.get(identifier);
			logger.log(Level.INFO, "Closing socket.");
			// Close the socket.
			socket.close();
			sockets.remove(identifier);
		} catch (IOException e) {
			// Closing the socket failed due to an IOException.
			logger.log(Level.WARNING, "Received an IOException while closing the socket for identifier = " + identifier + ": " + e.getMessage());
			return DisconnectResponse.FAIL;
		}

		return DisconnectResponse.SUCCESS;
	}

	/**
	 * Returns the local port number on which the local hidden service runs.
	 *
	 * @return The port number of the local hidden service.
	 */
	public int localPort() {
		return receiver.getPort();
	}


	/**
	 * Creates a Tor hidden service by connecting to the Tor control port and invoking JTorCtl. The directory of the hidden service must already be present.
	 *
	 * @throws IOException Throws an IOException when the Tor control socket is not reachable, or if the Tor authentication fails.
	 */
	private void createHiddenService() throws IOException {
		logger.log(Level.INFO, "Creating hidden service.");
		logger.log(Level.INFO, "Opening socket on " + Constants.localhost + ":" + configuration.getTorControlPort() + " to control Tor.");
		// Connect to the Tor control port.
		Socket s = new Socket(Constants.localhost, configuration.getTorControlPort());
		logger.log(Level.INFO, "Fetching JTorCtl connection.");
		TorControlConnection conn = TorControlConnection.getConnection(s);
		logger.log(Level.INFO, "Authenticating the connection.");
		// Authenticate the connection.
		conn.authenticate(configuration.getAuthenticationBytes());

		// Set the properties for the hidden service configuration.
		LinkedList<String> properties = new LinkedList<String>();
		File hiddenServiceDirectory = new File(configuration.getHiddenServiceDirectory());

		// Read the hidden service directories in the hidden service root directory, and add them to the new hidden service configuration.
		for (File hiddenService : hiddenServiceDirectory.listFiles()) {
			// Skip over any files in the directory.
			if (!hiddenService.isDirectory()) continue;

			// Skip over any directories without the hidden service prefix.
			String name = hiddenService.getName();
			if (!name.startsWith(Constants.hiddenserviceprefix));

			// Parse the port number from the directory name.
			String portString = name.substring(Constants.hiddenserviceprefix.length(), name.length());
			int port = Integer.valueOf(portString).intValue();

			// Add the hidden service property to the configuration properties so far.
			properties.add(Constants.hsdirkeyword + " " + hiddenService.getAbsolutePath());
			properties.add(Constants.hsportkeyword + " " + configuration.getHiddenServicePort() + " " + Constants.localhost + ":" + port);
		}

		logger.log(Level.INFO, "Setting configuration:" + Constants.newline + properties.toString());
		conn.setConf(properties);

		logger.log(Level.INFO, "Created hidden service.");
	}

	/**
	 * Deletes the hidden service directory.
	 *
	 * @throws IOException Propagates any IOException that occured during deletion.
	 */
	private void deleteHiddenService() throws IOException {
		// If the hidden service was not created, there is nothing to delete.
		if (directory == null) return;

		logger.log(Level.INFO, "Deleting hidden service directory.");
		File hostname = new File(configuration.getHiddenServiceDirectory() + File.separator + directory + File.separator + Constants.hostname);
		File hiddenservice = new File(configuration.getHiddenServiceDirectory() + File.separator + directory);
		File privatekey = new File(configuration.getHiddenServiceDirectory() + File.separator + directory + File.separator + Constants.prkey);

		boolean hostnameDeleted = hostname.delete();
		logger.log(Level.INFO, "Deleted hostname file: " + (hostnameDeleted ? "yes" : "no"));
		boolean prkeyDeleted = privatekey.delete();
		logger.log(Level.INFO, "Deleted private key file: " + (prkeyDeleted ? "yes" : "no"));
		boolean directoryDeleted = hiddenservice.delete();
		logger.log(Level.INFO, "Deleted hidden service directory: " + (directoryDeleted ? "yes" : "no"));

		if (!directoryDeleted || !hostnameDeleted || !prkeyDeleted)
			throw new IOException("Client failed to delete hidden service directory: " + hiddenservice.getAbsolutePath());
	}

	/**
	 * Reads the Tor hidden service identifier from the hostname file.
	 *
	 * @param hostname The file from which the Tor hidden service identifier should be read.
	 * @return The string identifier representing the Tor hidden service identifier.
	 * @throws IOException Throws an IOException when unable to read the Tor hidden service hostname file.
	 */
	private String readIdentifier(File hostname) throws IOException {
		logger.log(Level.INFO, "Reading identifier from file: " + hostname);
		FileInputStream stream = new FileInputStream(hostname);
		InputStreamReader reader = new InputStreamReader(stream);
		BufferedReader buffer = new BufferedReader(reader);

		logger.log(Level.INFO, "Reading line.");
		String identifier = buffer.readLine();

		logger.log(Level.INFO, "Closing file stream.");
		buffer.close();

		logger.log(Level.INFO, "Read identifier: " + identifier);
		return identifier;
	}


	/**
	 * Returns the connection listener which adds new incoming connections to the known connections.
	 *
	 * @return The connection listener which handles new incoming connections.
	 */
	private ConnectionListener getConnectionListener() {
		return new ConnectionListener() {

			/**
			 * @see ConnectionListener
			 */
			@Override
			public void ConnectionOpen(Identifier origin, Socket socket) {
				// Add the connection to the socket set.
				sockets.put(origin.getTorAddress(), socket);
			}

		};
	}

}
