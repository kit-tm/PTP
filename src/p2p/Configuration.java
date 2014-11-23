package p2p;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;


/**
 * Holds the P2P over Tor configuration. This includes:
 * 		* directory of the Tor hidden service
 * 		* port of the Tor hidden service
 * 		* Tor control port
 * 		* Tor socks proxy port
 * 		* authentication bytes for the Tor control protocol
 * 		* timeout for connections to hidden service identifiers
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
	public static final String HiddenServicePort = "HiddenServicePort";
	// TODO: eventually support authentication types
	//public static final String AuthenticationType = "AuthenticationType";
	public static final String ConnectionPoll = "ConnectionPoll";
	public static final String SocketConnectTimeout = "SocketConnectTimeout";
	public static final String SocketTTL = "SocketTTL";
	public static final String SocketTTLPoll = "TTLPoll";
	public static final String LoggerConfigFile = "LoggerConfigFile";


	/** The logger configuration file. */
	private final String loggerConfiguration;
	/** The path of the working directory. */
	private final String workingDirectory;
	/** The path of the Tor hidden service directory. */
	private final String hiddenServiceDirectory;
	/** The port on which the hidden service should be available. */
	private final int hiddenServicePort;
	/** The port number of the Tor control socket. */
	private final int torControlPort;
	/** The port number of the Tor SOCKS proxy. */
	private final int torSOCKSProxyPort;
	/** The authentication bytes needed by a control connection to Tor. */
	private final byte[] authenticationBytes;
	/** The interval (in milliseconds) at which the API wrapper will attempt a connection to a hidden service identifier */
	private final int connectionPoll;
	/** The timeout (in milliseconds) for a socket connection to a hidden service identifier. */
	private final int socketTimeout;
	/** The TTL (in milliseconds) for a socket connection to a hidden service identifier. */
	private final int socketTTL;
	/** The interval (in milliseconds) at each the TTL of all sockets is checked. */
	private final int ttlPoll;


	/**
	 * Constructor method. Reads the configuration from a file.
	 *
	 * @param configurationFilename The path and name of the configuration file.
	 * @param portFilename The path and name of the Tor control port output file.
	 * @param controlPort The Tor control port number.
	 * @param socksPort The Tor SOCKS proxy port number.
	 * @throws IllegalArgumentException Throws an IllegalArgumentException if unable to parse or find a value.
	 * @throws IOException Throws an IOException if unable to read or find the input configuration or control port file.
	 */
	public Configuration(String configurationFilename, String workingDirectory, int controlPort, int socksPort) throws IllegalArgumentException, IOException {
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

		torControlPort = controlPort;
		torSOCKSProxyPort = socksPort;

		// Check if the configuration file contains an entry for the logger configuration.
		if (properties.containsKey(LoggerConfigFile)) {
			loggerConfiguration = properties.get(LoggerConfigFile);
			System.setProperty(Constants.loggerconfig, loggerConfiguration);
			System.out.println("here");
		} else
			loggerConfiguration = "";
		// Create the logger AFTER its configuration file has been set.
		logger = Logger.getLogger(Constants.configlogger);
		logger.info("Set the logger properties file to: " + loggerConfiguration);

		// Check if all the needed properties are in the configuration file.
		check(properties, HiddenServicePort);
		check(properties, SocketConnectTimeout);
		check(properties, SocketTTL);
		check(properties, SocketTTLPoll);


		// Set the configuration parameters.

		this.workingDirectory = workingDirectory;
		logger.info("Set the working directory to: " + this.workingDirectory);

		hiddenServiceDirectory = workingDirectory + File.separator + Constants.hiddenservicedir;
		logger.info("Set the hidden servide directory to: " + hiddenServiceDirectory);

		hiddenServicePort = parse(properties, HiddenServicePort);
		logger.info("Read " + HiddenServicePort + " = " + hiddenServicePort);

		authenticationBytes = new byte[0];

		connectionPoll = parse(properties, ConnectionPoll);
		logger.info("Read " + ConnectionPoll + " = " + connectionPoll);

		socketTimeout = parse(properties, SocketConnectTimeout);
		logger.info("Read " + SocketConnectTimeout + " = " + socketTimeout);

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

		sb.append("\thidden service directory = ");
		sb.append(hiddenServiceDirectory);
		sb.append("\n");

		sb.append("\thidden service port number = ");
		sb.append(hiddenServicePort);
		sb.append("\n");

		sb.append("\tTor control port number = ");
		sb.append(torControlPort);
		sb.append("\n");

		sb.append("\tsocket connection timeout = ");
		sb.append(connectionPoll);
		sb.append("\n");

		sb.append("\tsocket connection timeout = ");
		sb.append(socketTimeout);
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
	 * Returns the TorP2P working directory as specified.
	 *
	 * @return The TorP2P working directory.
	 */
	public String getWorkingDirectory() { return workingDirectory; }

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
	 * Returns the bytes needed by a Tor authentication message.
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
	 * Returns the interval (in milliseconds) at which the API wrapper will attempt a connection to a hidden service identifier.
	 *
	 * @return The socket connection timeout.
	 */
	public int getConnectionPoll() { return connectionPoll; }

	/**
	 * Returns the socket connection timeout (in milliseconds) when connecting to a hidden service identifier.
	 *
	 * @return The socket connection timeout.
	 */
	public int getSocketTimeout() { return socketTimeout; }

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
