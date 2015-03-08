package api;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;

import utility.Constants;


/**
 * Holds the P2P over Tor configuration. This includes:
 *
 *		* hidden service port number
 *		* interval at which a connection to a hidden service identifier is attempted
 *		* timeout for socket connections
 *		* socket poll for available data interval
 *		* connection TTL
 *		* interval at which socket remaining TTL is checked
 *		* Tor bootstrapping timeout
 *		* number of threads to use for message dispatching
 *		* number of threads to use for message receiving
 *		* default hidden service identifier
 *		* logger configuration file
 *
 * @author Simeon Andreev
 *
 */
public class Configuration {

	/** The logger for this class. */
	protected final Logger logger;

	// Expose constants for testing.

	/** The delimiter used in the configuration file to separate property keys and values. */
	public static final String delimiter = " ";
	/** The Symbol used for commented lines in the configuration file. */
	public static final String comment = "#";

	/** Configuration file property names. */
	public static final String DefaultIdentifier = "DefaultIdentifier";
	public static final String HiddenServicePort = "HiddenServicePort";
	// TODO: eventually support authentication types
	//public static final String AuthenticationType = "AuthenticationType";
	public static final String TorBootstrapTimeout = "TorBootstrapTimeout";
	public static final String DispatcherThreadsNumber = "DispatcherThreads";
	public static final String ReceiverThreadsNumber = "ReceiverThreads";
	public static final String SocketConnectTimeout = "SocketConnectTimeout";
	public static final String SocketReceivePoll = "SocketReceivePoll";
	public static final String SocketTTL = "SocketTTL";
	public static final String SocketTTLPoll = "TTLPoll";
	public static final String LoggerConfigFile = "LoggerConfigFile";


	/** The logger configuration file. */
	private final String loggerConfiguration;
	/** The default hidden service identifier. */
	private final String defaultIdentifier;
	/** The port on which the hidden service should be available. */
	private final int hiddenServicePort;
	/** The authentication bytes needed by a control connection to Tor. */
	private final byte[] authenticationBytes;
	/** The timeout (in milliseconds) for the Tor bootstrapping. */
	private final int bootstrapTimeout;
	/** The maximum number of message dispatcher threads. */
	private final int dispatcherThreadsNumber;
	/** The maximum number of message receiver threads. */
	private final int receiverThreadsNumber;
	/** The timeout (in milliseconds) for a socket connection to a hidden service identifier. */
	private final int socketTimeout;
	/** The interval (in milliseconds) at which the open sockets are polled for incoming data. */
	private final int receivePoll;
	/** The TTL (in milliseconds) for a socket connection to a hidden service identifier. */
	private final int socketTTL;
	/** The interval (in milliseconds) at each the TTL of all sockets is checked. */
	private final int ttlPoll;

	/** The path of the working directory. */
	private String workingDirectory;
	/** The path of the Tor hidden service directory. */
	private String hiddenServiceDirectory;
	/** The port number of the Tor control socket. */
	private int torControlPort;
	/** The port number of the Tor SOCKS proxy. */
	private int torSOCKSProxyPort;


	/**
	 * Constructor method. Reads the configuration from a file.
	 *
	 * @param configurationFilename The path and name of the configuration file.
	 * @throws IllegalArgumentException Throws an IllegalArgumentException if unable to parse or find a value.
	 * @throws IOException Throws an IOException if unable to read or find the input configuration or control port file.
	 */
	public Configuration(String configurationFilename) throws IllegalArgumentException, IOException {
		File configuration = new File(configurationFilename);

		if (!configuration.exists())
			throw new IllegalArgumentException("Configuration file does not exist: " + configurationFilename);

		FileReader reader = new FileReader(configuration);
		BufferedReader buffer = new BufferedReader(reader);
		HashMap<String, String> properties = new HashMap<String, String>();

		// Read the entries of the configuration file in the map.
		int n = 0;
		while (buffer.ready()) {
			String line = buffer.readLine();
			++n;

			// Skip empty lines.
			if (line.isEmpty()) continue;
			// Skip commented lines.
			if (line.startsWith(comment)) continue;

			String[] pair = line.split(delimiter);

			// Entries must be key value pairs separated by the delimiter.
			if (pair.length != 2) {
				buffer.close();
				throw new IllegalArgumentException("Configuration file line " + n + " must be in the form: key" + delimiter + "value");
			}

			// Add the entry.
			properties.put(pair[0], pair[1]);
		}

		buffer.close();

		// Check if the configuration file contains an entry for the logger configuration.
		if (properties.containsKey(LoggerConfigFile)) {
			loggerConfiguration = properties.get(LoggerConfigFile);
			System.setProperty(Constants.loggerconfig, loggerConfiguration);
		} else
			loggerConfiguration = "";
		// Create the logger AFTER its configuration file has been set.
		logger = Logger.getLogger(Constants.configlogger);
		logger.info("Set the logger properties file to: " + loggerConfiguration);

		// Check if all the needed properties are in the configuration file.
		check(properties, DefaultIdentifier);
		check(properties, HiddenServicePort);
		check(properties, TorBootstrapTimeout);
		check(properties, SocketConnectTimeout);
		check(properties, SocketReceivePoll);
		check(properties, SocketTTL);
		check(properties, SocketTTLPoll);


		// Set the configuration parameters.
		defaultIdentifier = properties.get(DefaultIdentifier);
		logger.info("Read " + DefaultIdentifier + " = " + defaultIdentifier);

		hiddenServicePort = parse(properties, HiddenServicePort);
		logger.info("Read " + HiddenServicePort + " = " + hiddenServicePort);

		authenticationBytes = new byte[0];

		bootstrapTimeout = parse(properties, TorBootstrapTimeout);
		logger.info("Read " + TorBootstrapTimeout + " = " + bootstrapTimeout);

		dispatcherThreadsNumber = parse(properties, DispatcherThreadsNumber);
		logger.info("Read " + DispatcherThreadsNumber + " = " + dispatcherThreadsNumber);

		receiverThreadsNumber = parse(properties, ReceiverThreadsNumber);
		logger.info("Read " + ReceiverThreadsNumber + " = " + receiverThreadsNumber);

		socketTimeout = parse(properties, SocketConnectTimeout);
		logger.info("Read " + SocketConnectTimeout + " = " + socketTimeout);

		receivePoll = parse(properties, SocketReceivePoll);
		logger.info("Read " + SocketReceivePoll + " = " + receivePoll);

		socketTTL = parse(properties, SocketTTL);
		logger.info("Read " + SocketTTL + " = " + socketTTL);

		ttlPoll = parse(properties, SocketTTLPoll);
		logger.info("Read " + SocketTTLPoll + " = " + ttlPoll);
	}


	/**
	 * Returns a formatted list of all configuration values.
	 *
	 * @return A string containing formatted configuration values.
	 * @see Object
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("<Configuration>\n");

		sb.append("\tworking directory = ");
		sb.append(workingDirectory);
		sb.append("\n");

		sb.append("\tdefault identifier = ");
		sb.append(defaultIdentifier);
		sb.append("\n");

		sb.append("\thidden service directory = ");
		sb.append(hiddenServiceDirectory);
		sb.append("\n");

		sb.append("\thidden service port number = ");
		sb.append(hiddenServicePort);
		sb.append("\n");

		sb.append("\tTor bootstrap timeout = ");
		sb.append(bootstrapTimeout);
		sb.append("\n");

		sb.append("\tTor control port number = ");
		sb.append(torControlPort);
		sb.append("\n");

		sb.append("\tnumber of dispatcher threads = ");
		sb.append(dispatcherThreadsNumber);
		sb.append("\n");

		sb.append("\tnumber of receiver threads = ");
		sb.append(receiverThreadsNumber);
		sb.append("\n");

		sb.append("\tsocket connection timeout = ");
		sb.append(socketTimeout);
		sb.append("\n");

		sb.append("\tsocket receive poll = ");
		sb.append(SocketReceivePoll);
		sb.append("\n");

		sb.append("\tsocket connection TTL = ");
		sb.append(socketTTL);
		sb.append("\n");

		sb.append("\tTTL poll = ");
		sb.append(ttlPoll);
		sb.append("\n");

		sb.append("\tlogger configuration file = ");
		sb.append(loggerConfiguration);
		sb.append("\n");

		sb.append("</Configuration>");

		return sb.toString();
	}


	/**
	 * Sets the Tor control and SOCKS port numbers of the configuration.
	 *
	 * @param directory The working directory of the Tor process.
	 * @param controlPort The Tor control port number.
	 * @param socksPort The Tor SOCKS port number.
	 */
	public void setTorConfiguration(String directory, int controlPort, int socksPort) {
		torControlPort = controlPort;
		logger.info("Set the Tor control port to: " + torControlPort);

		torSOCKSProxyPort = socksPort;
		logger.info("Set the Tor SOCKS port to: " + torSOCKSProxyPort);

		workingDirectory = directory;
		logger.info("Set the working directory to: " + this.workingDirectory);

		hiddenServiceDirectory = workingDirectory + File.separator + Constants.hiddenservicedir;
		logger.info("Set the hidden servide directory to: " + hiddenServiceDirectory);
	}


	/**
	 * Returns the TorP2P working directory as specified.
	 *
	 * @return The TorP2P working directory.
	 */
	public String getWorkingDirectory() { return workingDirectory; }

	/**
	 * Returns the default Tor hidden service identifier.
	 *
	 * @return The default Tor hidden service identifier.
	 */
	public String getDefaultIdentifier() { return defaultIdentifier; }

	/**
	 * Returns the Tor hidden service directory as specified.
	 *
	 * @return The Tor hidden service directory.
	 */
	public String getHiddenServiceDirectory() { return hiddenServiceDirectory; }

	/**
	 * Returns the Tor hidden service port as specified.
	 *
	 * @return The Tor hidden service port.
	 */
	public int getHiddenServicePort() { return hiddenServicePort; }

	/**
	 * Returns the Tor bootstrap timeout (in milliseconds).
	 *
	 * @return The Tor bootstrap timeout.
	 */
	public int getTorBootstrapTimeout() { return bootstrapTimeout; }

	/**
	 * Returns the bytes needed by the Tor authentication message.
	 *
	 * @return The Tor authentication bytes.
	 * @see https://gitweb.torproject.org/torspec.git/blob/HEAD:/control-spec.txt
	 */
	public byte[] getAuthenticationBytes() { return authenticationBytes; }

	/**
	 * Returns the Tor control socket port as specified.
	 *
	 * @return The Tor control sockert port.
	 */
	public int getTorControlPort() { return torControlPort; }

	/**
	 * Returns the Tor SOCKS proxy port as specified.
	 *
	 * @return The Tor control sockert port.
	 */
	public int getTorSOCKSProxyPort() { return torSOCKSProxyPort; }

	/**
	 * Returns the maximum number of message dispatcher threads.
	 *
	 * @return The socket connection timeout.
	 */
	public int getDispatcherThreadsNumber() { return dispatcherThreadsNumber; }

	/**
	 * Returns the maximum number of message receiver threads.
	 *
	 * @return The socket connection timeout.
	 */
	public int getReceiverThreadsNumber() { return receiverThreadsNumber; }

	/**
	 * Returns the socket connection timeout (in milliseconds) when connecting to a hidden service identifier.
	 *
	 * @return The socket connection timeout.
	 */
	public int getSocketTimeout() { return socketTimeout; }

	/**
	 * Returns the interval (in milliseconds) at which open sockets are polled for incoming data.
	 *
	 * @return The socket read poll interval.
	 */
	public int getSocketReceivePoll() { return receivePoll; }

	/**
	 * Returns the socket TTL (in milliseconds) of a connection to a hidden service identifier.
	 *
	 * @return The socket TTL of a connection to a hidden service identifier.
	 */
	public int getSocketTTL() { return socketTTL; }

	/**
	 * Returns the interval (in milliseconds) at which the TTL of all sockets is checked.
	 *
	 * @return The interval at which the TTL of all sockets is checked.
	 */
	public int getTTLPoll() { return ttlPoll; }


	/**
	 * Check if a string-to-string hash map contains a specific key. Will throw an illegal argument exception if not.
	 *
	 * @param map The hash map to be checked.
	 * @param key The key to be looked for in the map.
	 * @throws IllegalArgumentException Thrown when the map does not contain the specified key.
	 */
	private void check(HashMap<String, String> map, String key) throws IllegalArgumentException {
		logger.info("Checking if the configuration file contains the " + key + " property.");
		if (!map.containsKey(key)) throw new IllegalArgumentException("Configuration file does not contain the " + key + " property.");
	}

	/**
	 * Parses the integer value of a specific key in a string-to-string hash map.
	 *
	 * @param map The hash map containing the key value pair.
	 * @param key The key of the value to be parsed.
	 * @throws IllegalArgumentException Thrown when the value can not be parsed.
	 */
	private int parse(HashMap<String, String> map, String key) throws IllegalArgumentException {
		logger.info("Parsing integer value of the " + key + " property: " + map.get(key));
		int value = 0;

		try {
			value = Integer.valueOf(map.get(key));
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Could not parse the integer value of the " + key + " property.");
		}

		return value;

	}

}
