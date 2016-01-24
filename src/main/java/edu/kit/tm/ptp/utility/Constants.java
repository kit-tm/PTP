package edu.kit.tm.ptp.utility;


/**
 * Contains some constants: * Tor hidden service hostnaem filename * Tor configuration file hidden
 * service directory keyword * Tor configuration file hidden service port keyword * the localhost
 * address * .onion extension of Tor addresses * maximum buffer length when reading a message from
 * the local socket * logger names for the different classes * server socket accept socket
 * connection timeout * logger properties file * configuration .ini file
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
  public static final String torfile = "tor";
  /** The path to and the name of the Tor configuration file. */
  public static final String torrcfile = "config/torrc";
  /** The Tor executable option for a custom input torrc file. */
  public static final String torrcoption = "-f";
  /** The Tor configuration option for the working directory property. */
  public static final String datadiroption = "--DataDirectory";
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

  /** A delimiter to separate message length from message content. */
  public static final char messagelengthdelimiter = '.';
  /** A flag to indicate a message origin. */
  public static final char messageoriginflag = 'o';
  /** A flag to indicate a disconnection message. */
  public static final char messagedisconnectflag = 'd';
  /** A flag to indicate a standard message. */
  public static final char messagestandardflag = 's';
  /** The path to and the name of the configuration file. */
  public static final String configfile = "config/ptp.ini";
  /** The timestamp format used as a prefix for the temporary Tor working directory creation. */
  public static final String timestampformat = "yyMMddHHmmss";
  /** The name of the hidden service root directory. */
  public static final String hiddenservicedir = "hidden_services";
  /** The name of the file in which the port of a hidden service is held. */
  public static final String portfile = "port";
  /** The prefix of the hidden service directories. */
  public static final String hiddenserviceprefix = "hs";
  /** The name of the PeerTorPeer home environment variable. */
  public static final String ptphome = "PTP_HOME";
  /** The path of the default PeerTorPeer home directory. */
  public static final String ptphomedefault = "./ptp-data";
  /** The name of the Tor manager ports file. */
  public static final String tormanagerportsfile = "PTPTorManagerPorts";
  /** The name of the Tor manager lock file. */
  public static final String tormanagerlockfile = "PTPTorManagerLock";
  /** The name of the raw API lock file. */
  public static final String rawapilockfile = "PTPRawAPILock";
  /** The name of the lock file for a hidden service.*/
  public static final String hiddenservicelockfile = "PTPHSLock";
  /** The Tor hidden service origin identifier placeholder when no information is available. */
  public static final String niaorigin = "N/A";

  /* Logger constants */

  /** The java logger config file system property name. */
  public static final String loggerconfig = "java.util.logging.config.file";

  /** The name of the logger for the PTP class. */
  public static final String ptplogger = "edu.kit.tm.ptp.PTP";
  /** The name of the logger for the Client class. */
  public static final String clientlogger = "edu.kit.tm.ptp.raw.Client";
  /** The name of the logger for the Waiter class. */
  public static final String waiterlogger = "edu.kit.tm.ptp.raw.threads.Waiter";
  /** The name of the logger for the socket TTL manager class. */
  public static final String managerlogger = "edu.kit.tm.ptp.raw.threads.TTLManager";
  /** The name of the logger for the socket TTL manager class. */
  public static final String dispatcherlogger = "edu.kit.tm.ptp.raw.threads.Dispatcher";
  /** The name of the logger for the socket TTL manager class. */
  public static final String receiverlogger = "edu.kit.tm.ptp.raw.threads.Receiver";
  /** The name of the logger for the Configuration class. */
  public static final String configlogger = "edu.kit.tm.ptp.raw.Configuration";
  /** The name of the logger for the MessageHandler class. */
  public static final String receivethreadlogger = "edu.kit.tm.ptp.ReceiveThread";
  /** The name of the logger for the ConnectionManager class. */
  public static final String connectionManagerLogger = "edu.kit.tm.ptp.ConnectionManager";



  /* JVM constants */

  /** The port number which tells the JVM to pick any available port for a server socket. */
  public static final int anyport = 0;
  /** The localhost address. */
  public static final String localhost = "127.0.0.1";
  /** The working directory Java system property keyword. */
  public static final String userdir = "user.dir";
  /** File read+write rights. */
  public static final String readwriterights = "rw";
  /** Newline symbol. */
  public static final String newline = "\n";

}
