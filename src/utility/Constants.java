package utility;


/**
 * Contains some constants:
 * 		* Tor hidden service hostnaem filename
 * 		* Tor configuration file hidden service directory keyword
 * 		* Tor configuration file hidden service port keyword
 *		* the localhost address
 *		* .onion extension of Tor addresses
 *		* maximum buffer length when reading a message from the local socket
 *		* logger names for the different classes
 *		* server socket accept socket connection timeout
 *		* logger properties file
 *		* configuration .ini file
 *
 * @author Simeon Andreev
 *
 */
public class Constants {


	/* Tor general constants */

	/** The name of the Tor hostname file. */
	public static final String hostname = "hostname";
	/** The name of the Tor private key file. */
	public static final String prkey = "private_key";
	/** The Tor configuration keyword for the hidden service directory property. */
	public static final String hsdirkeyword = "HiddenServiceDir";
	/** The Tor configuration keyword for the hidden service port property. */
	public static final String hsportkeyword = "HiddenServicePort";
	/** The Tor configuration keyword for the SOCKS proxy port property. */
	public static final String torsocksportkeyword = "SocksPort";
	/** The .onion extension of Tor addresses. */
	public static final String onion = ".onion";
	/** The port delimiter in the Tor control port output file. */
	public static final String portdelimiter = ":";


	/* Tor executable constants. */

	/** The name of the Tor executable file, which should be on the PATH system variable. */
	public static String torfile = "tor";
	/** The path to and the name of the Tor configuration file. */
	public static String torrcfile = "config/torrc";
	/** The Tor executable option for a custom input torrc file. */
	public static final String torrcoption = "-f";
	/** The Tor configuration option for the working directory property. */
	public static String datadiroption = "--DataDirectory";
	/** The Tor configuration option for the working directory property. */
	public static final String ctlportoutoption = "--ControlPortWriteToFile";
	/** A part of the Tor executable log message, logged when the bootstrapping is complete. */
	public static final String bootstrapdone = "Bootstrapped 100%";
	/** A part of the Tor executable log message, logged when the Tor control port is open. */
	public static final String controlportopen = "Control listener listening on port";
	/** A part of the Tor executable log message, logged when the Tor SOCKS proxy port is open. */
	public static final String socksportopen = "Socks listener listening on port";
	/** The shutdown signal used when shutting down Tor with JTorCtl. */
	public static final String shutdownsignal = "SHUTDOWN";


	/* API constants. */

	/** The maximum length of a message that can be received on the local socket. */
	public static final int maxlength = 1024;
	/** The path to and the name of the configuration file. */
	public static String configfile = "config/p2p.ini";
	/** The timestamp format used as a prefix for the temporary Tor working directory creation. */
	public static final String timestampformat = "yyMMddHHmmss";
	/** The name of the hidden service root directory. */
	public static final String hiddenservicedir = "hidden_services";
	/** The prefix of the hidden service directories. */
	public static final String hiddenserviceprefix = "hs";
	/** The name of the TorP2P home environment variable. */
	public static final String torp2phome = "TORP2P_HOME";
	/** The path of the default TorP2P home directory. */
	public static String torp2phomedefault = "./torp2p-data";
	/** The name of the TorP2P Tor manager ports file. */
	public static final String tormanagerportsfile = "TorP2PTorManagerPorts";
	/** The name of the TorP2P Tor manager lock file. */
	public static final String tormanagerlockfile = "TorP2PTorManagerLock";
	/** The name of the raw API lock file. */
	public static final String rawapilockfile = "TorP2PRawAPILock";


	/* Logger constants */

	/** The java logger config file system property name. */
	public static final String loggerconfig = "java.util.logging.config.file";

	/** The name of the logger for the TorP2P class. */
	public static final String torp2plogger = "p2p.TorP2P";
	/** The name of the logger for the Client class. */
	public static final String clientlogger = "p2p.Client";
	/** The name of the logger for the Waiter class. */
	public static final String waiterlogger = "threads.Waiter";
	/** The name of the logger for the socket TTL manager class. */
	public static final String managerlogger = "threads.TTLManager";
	/** The name of the logger for the socket TTL manager class. */
	public static final String dispatcherlogger = "threads.Dispatcher";
	/** The name of the logger for the Configuration class. */
	public static final String configlogger = "p2p.Configuration";


	/* JVM constants */

	/** The port number which tells the JVM to pick any available port for a server socket. */
	public static final int anyport = 0;
	/** The localhost address */
	public static final String localhost = "127.0.0.1";
	/** The working directory Java system property keyword. */
	public static final String userdir = "user.dir";
	/** File read+write rights. */
	public static final String readwriterights = "rw";
	/** Newline symbol. */
	public static final String newline = "\n";

}
