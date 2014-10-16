package p2p;


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
	public static final String filename = "hostname";
	/** The name of the Tor private key file. */
	public static final String prkey = "private_key";
	/** The Tor configuration keyword for the hidden service directory property. */
	public static final String hsdirkeyword = "HiddenServiceDir";
	/** The Tor configuration keyword for the hidden service port property. */
	public static final String hsportkeyword = "HiddenServicePort";
	/** The Tor configuration keyword for the SOCKs proxy port property. */
	public static final String torsocksportkeyword = "TorSocksProxyPort";
	/** The .onion extension of Tor addresses. */
	public static final String onion = ".onion";
	/** The port delimiter in the Tor control port output file. */
	public static final String portdelimiter = ":";


	/* Tor executable constants. */

	/** The name of the Tor executable file, which should be on the PATH system variable. */
	public static final String torfile = "tor";
	/** The path to and the name of the Tor configuration file. */
	public static final String torrcfile = "config/torrc";
	/** The Tor executable option for a custom input torrc file. */
	public static final String torrcoption = "-f";
	/** The name of the Tor control port output file. */
	public static final String portfile = "port.conf";
	/** The Tor configuration option for the working directory property. */
	public static final String datadiroption = "--DataDirectory";
	/** The Tor configuration option for the working directory property. */
	public static final String ctlportoutoption = "--ControlPortWriteToFile";


	/* API constants. */

	/** The maximum length of a message that can be received on the local socket. */
	public static final int maxlength = 1024;
	/** The path to and the name of the configuration file. */
	public static final String configfile = "config/p2p.ini";
	/** The path to and the name of the dummy Tor control port output file. */
	public static final String dummyportfile = "config/port.conf";
	/** The timestamp format used as a prefix for the temporary Tor working directory creation. */
	public static final String timestampformat = "yyMMddHHmmss";
	/** The name of the hidden service directory. */
	public static final String hiddenservicedir = "hidden_service";


	/* Logger constants */

	/** The java logger config file system property name. */
	public static final String loggerconfig = "java.util.logging.config.file";

	/** The name of the logger for the Client class. */
	public static final String clientlogger = "p2p.Client";
	/** The name of the logger for the Waiter class. */
	public static final String waiterlogger = "threads.Waiter";
	/** The name of the logger for the socket TTL manager class. */
	public static final String managerlogger = "threads.TTLManager";
	/** The name of the logger for the Configuration class. */
	public static final String configlogger = "p2p.Configuration";


	/* JVM constants */

	/** The port number which tells the JVM to pick any available port for a server socket. */
	public static final int anyport = 0;
	/** The localhost address */
	public static final String localhost = "127.0.0.1";
	/** The working directory Java system property keyword. */
	public static final String userdir = "user.dir";

}
