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
  /** The .onion extension of Tor addresses. */
  public static final String onion = ".onion";


  /* Tor executable constants. */

  /** The name of the Tor executable file, which should be on the PATH system variable. */
  public static final String torfile = "tor";
  /** The path to and the name of the Tor configuration file. */
  public static final String torrcfile = "config/torrc";
  public static final String testtorrcfile = "config/testtorrc";
  /** The Tor executable option for a custom input torrc file. */
  public static final String torrcoption = "-f";
  /** The Tor configuration option for the working directory property. */
  public static final String datadiroption = "--DataDirectory";
  /** The Tor configuration option for the working directory property. */
  public static final String ctlportwriteoption = "--ControlPortWriteToFile";
  /** The shutdown signal used when shutting down Tor with JTorCtl. */
  public static final String shutdownsignal = "SHUTDOWN";


  /* API constants. */

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
  /** The name of the Tor manager lock file. */
  public static final String tormanagerlockfile = "PTPTorManagerLock";
  /** The name of the raw API lock file. */
  public static final String rawapilockfile = "PTPRawAPILock";
  /** The name of the lock file for a hidden service.*/
  public static final String hiddenservicelockfile = "PTPHSLock";
  /** The time to wait for Tor to start.*/
  public static final long torStartTimeout = 10 * 1000;
  /** The time to wait for Tor to create a hidden service. */
  public static final long torCreateHiddenServiceTimeout = 5 * 1000;

  public static final String torDisableNetwork = "DisableNetwork";
  public static final String torControlPortFileName = "control";
  public static final String torControlPortWriteToFile = "ControlPortWriteToFile";
  public static final String torGetInfoSOCKSProxy = "net/listeners/socks";

  /* Logger constants */

  /** The java logger config file system property name. */
  public static final String loggerconfig = "java.util.logging.config.file";

  /* JVM constants */

  /** The port number which tells the JVM to pick any available port for a server socket. */
  public static final int anyport = 0;
  public static final int maxport = 65535;
  /** The localhost address. */
  public static final String localhost = "127.0.0.1";
  /** File read+write rights. */
  public static final String readwriterights = "rw";
  /** Newline symbol. */
  public static final String newline = "\n";
  public static final String charset = "UTF-8";

}
