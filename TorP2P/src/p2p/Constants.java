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

	/** The name of the Tor hostname file. */
	public static final String filename = "hostname";
	/** The name of the Tor private key file. */
	public static final String prkey = "private_key";
	/** The Tor configuration file keyword for the hidden service directory property. */
	public static final String hsdirkeyword = "HiddenServiceDir";
	/** The Tor configuration file keyword for the hidden service port property. */
	public static final String hsportkeyword = "HiddenServicePort";
	/** The localhost address */
	public static final String localhost = "127.0.0.1";
	/** The .onion extension of Tor addresses. */
	public static final String onion = ".onion";

	/** The maximum length of a message that can be received on the local socket. */
	public static final int maxlength = 1024;

	/** The name of the logger for the Client class. */
	public static final String clientlogger = "p2p.Client";
	/** The name of the logger for the Waiter class. */
	public static final String waiterlogger = "threads.Waiter";
	/** The name of the logger for the Configuration class. */
	public static final String configlogger = "p2p.Configuration";

	/** The path to and the name of the configuration file. */
	public static final String configfile = "config/p2p.ini";

	/** The java logger config file system property name. */
	public static final String loggerconfig = "java.util.logging.config.file";

}
