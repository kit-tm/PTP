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

	/** The delimiter used in the configuration file to separate property keys and values. */
	private static final String delimiter = " ";
	/** The Symbol used for commented lines in the configuration file. */
	private static final String comment = "#";
	/** Configuration file property names. */
	private static final String HiddenServiceDirectory = "HiddenServiceDirectory";
	private static final String HiddenServicePort = "HiddenServicePort";
	//private static final String AuthenticationType = "AuthenticationType";	// TODO: eventually support authentication types
	private static final String TorControlPort = "TorControlPort";
	private static final String TorSocksProxyPort = "TorSocksProxyPort";
	private static final String SocketConnectTimeout = "SocketConnectTimeout";
	private static final String LoggerConfigFile = "LoggerConfigFile";


	/** The authentication property value for no authentication. */
	//private static final String noauthentication = "none";
	/** The authentication property value for cookie authentication. */
	//private static final String cookieauthentication = "none";

	/** The logger configuration file. */
	private final String loggerConfiguration;
	/** The directory where Tor should create a hidden service. */
	private final String hiddenServiceDirectory;
	/** The port on which the hidden service should be available. */
	private final int hiddenServicePort;
	/** The bytes needed to authenticate a connection to Tors control socket. */
	private final byte[] authenticationBytes;
	/** The port number of the Tor control socket. */
	private final int torControlPort;
	/** The port number of the Tor socks proxy. */
	private final int torSocksProxyPort;
	/** The timeout for a socket connection to a hidden service identifier. */
	private final int socketTimeout;


	/**
	 * Constructor method. Reads the configuration from a file.
	 *
	 * @param filename The path and name of the file.
	 * @throws IllegalArgumentException Thrown if unable to parse or find a value.
	 * @throws IOException Thrown if unable to read or find the input file.
	 */
	public Configuration(String filename) throws IllegalArgumentException, IOException {
		File configuration = new File(filename);

		if (!configuration.exists())
			throw new IllegalArgumentException("Configuration file does not exist: " + filename);

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
		// Create the logger AFTER the configuration file has been set.
		logger = Logger.getLogger(Constants.configlogger);
		logger.info("Set the logger properties file to: " + loggerConfiguration);

		// Check if all the needed properties are in the configuration file.
		check(properties, HiddenServiceDirectory);
		check(properties, HiddenServicePort);
		//check(properties, AuthenticationType);
		check(properties, TorControlPort);
		check(properties, TorSocksProxyPort);
		check(properties, SocketConnectTimeout);

		// Set the configuration parameters.

		hiddenServiceDirectory = properties.get(HiddenServiceDirectory);
		logger.info("Read " + HiddenServiceDirectory + " = " + hiddenServiceDirectory);

		hiddenServicePort = parse(properties, HiddenServicePort);
		logger.info("Read " + HiddenServicePort + " = " + hiddenServicePort);

		authenticationBytes = new byte[0];
				//java.nio.file.Files.readAllBytes(java.nio.file.FileSystems.getDefault().getPath("C:\\Programme\\Tor Browser\\Data\\Tor\\control_auth_cookie"));

		//logger.info("Set " + AuthenticationType + " with bytes = " + authenticationBytes);

		torControlPort = parse(properties, TorControlPort);
		logger.info("Read " + TorControlPort + " = " + torControlPort);

		torSocksProxyPort = parse(properties, TorSocksProxyPort);
		logger.info("Read " + TorSocksProxyPort + " = " + torSocksProxyPort);

		socketTimeout = parse(properties, SocketConnectTimeout);
		logger.info("Read " + SocketConnectTimeout + " = " + socketTimeout);
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

		sb.append("\thidden service directiry = ");
		sb.append(hiddenServiceDirectory);
		sb.append("\n");

		sb.append("\thidden service port number = ");
		sb.append(hiddenServicePort);
		sb.append("\n");

		sb.append("\tauthentication bytes = ");
		sb.append(new String(authenticationBytes));
		sb.append("\n");

		sb.append("\tTor control port number = ");
		sb.append(torControlPort);
		sb.append("\n");

		sb.append("\tTor socks proxy port number = ");
		sb.append(torSocksProxyPort);
		sb.append("\n");

		sb.append("\tsocket connection timeout = ");
		sb.append(socketTimeout);
		sb.append("\n");

		sb.append("\tlogger configuration file = ");
		sb.append(loggerConfiguration);
		sb.append("\n");

		sb.append("</Configuration>");

		return sb.toString();
	}


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
	 * Returns the Tor socks proxy port as specified.
	 *
	 * @return The Tor socks proxy port.
	 */
	public int getTorSocksProxyPort() { return torSocksProxyPort; }

	/**
	 * Returns the socket timeout when connecting to a hidden service identifier.
	 *
	 * @return The socket connection timeout.
	 */
	public int getSocketTimeout() { return socketTimeout; }


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
		logger.info("Parsing integer value of the " + key + " property.");
		int value = 0;

		try {
			value = Integer.valueOf(map.get(key));
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Could not parse the integer value of the " + key + " property.");
		}

		return value;

	}

}
