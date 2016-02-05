package edu.kit.tm.ptp;

import edu.kit.tm.ptp.utility.Constants;

import java.io.File;
import java.util.logging.Logger;


/**
 * Holds the PeerTorPeer (PTP) configuration.
 * This includes: * hidden service port number * interval at which a connection to a hidden service
 * identifier is attempted * timeout for socket connections * socket poll for available data
 * interval * connection TTL * interval at which socket remaining TTL is checked * Tor bootstrapping
 * timeout * number of threads to use for message dispatching * number of threads to use for message
 * receiving * default hidden service identifier * logger configuration file
 *
 * @author Timon Hackenjos
 * @author Simeon Andreev
 *
 */
public class Configuration {

  /** The logger for this class. */
  private Logger logger = null;

  /** The logger configuration file. */
  private String loggerConfiguration;
  /** The default hidden service identifier. */
  private String defaultIdentifier;
  /** The port on which the hidden service should be available. */
  private int hiddenServicePort;
  /** The authentication bytes needed by a control connection to Tor. */
  private byte[] authenticationBytes;
  /** The timeout (in milliseconds) for the Tor bootstrapping. */
  private int bootstrapTimeout;
  /** The timeout (in milliseconds) for a socket connection to a hidden service identifier. */
  private int socketTimeout;
  /** The interval (in milliseconds) at which the open sockets are polled for incoming data. */
  private int receivePoll;
  /** The TTL (in milliseconds) for a socket connection to a hidden service identifier. */
  private int socketTtl;
  /** The interval (in milliseconds) at each the TTL of all sockets is checked. */
  private int ttlPoll;

  /** The path of the working directory. */
  private String workingDirectory;
  /** The path of the Tor hidden service directory. */
  private String hiddenServiceDirectory;
  /** The port number of the Tor control socket. */
  private int torControlPort;
  /** The port number of the Tor SOCKS proxy. */
  private int torSocksProxyPort;


  protected Configuration() {

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

    sb.append("\tsocket connection timeout = ");
    sb.append(socketTimeout);
    sb.append("\n");

    sb.append("\tsocket receive poll = ");
    sb.append(receivePoll);
    sb.append("\n");

    sb.append("\tsocket connection TTL = ");
    sb.append(socketTtl);
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
   * Sets the logger configuration and creates a logger object if none exists.
   */
  public void setLoggerConfiguration(String loggerConfiguration) {
    this.loggerConfiguration = loggerConfiguration;
    
    if (logger == null) {
      logger = Logger.getLogger(Configuration.class.getName());
    }
  }


  public void setBootstrapTimeout(int bootstrapTimeout) {
    this.bootstrapTimeout = bootstrapTimeout;
  }


  public void setSocketTtl(int socketTtl) {
    this.socketTtl = socketTtl;
  }


  public void setTtlPoll(int ttlPoll) {
    this.ttlPoll = ttlPoll;
  }


  public void setDefaultIdentifier(String defaultIdentifier) {
    this.defaultIdentifier = defaultIdentifier;
  }


  public void setHiddenServicePort(int hiddenServicePort) {
    this.hiddenServicePort = hiddenServicePort;
  }


  public void setAuthenticationBytes(byte[] authenticationBytes) {
    this.authenticationBytes = authenticationBytes;
  }


  public void setSocketTimeout(int socketTimeout) {
    this.socketTimeout = socketTimeout;
  }


  public void setWorkingDirectory(String workingDirectory) {
    this.workingDirectory = workingDirectory;
  }


  public void setHiddenServiceDirectory(String hiddenServiceDirectory) {
    this.hiddenServiceDirectory = hiddenServiceDirectory;
  }


  public void setTorControlPort(int torControlPort) {
    this.torControlPort = torControlPort;
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

    torSocksProxyPort = socksPort;
    logger.info("Set the Tor SOCKS port to: " + torSocksProxyPort);

    workingDirectory = directory;
    logger.info("Set the working directory to: " + this.workingDirectory);

    hiddenServiceDirectory = workingDirectory + File.separator + Constants.hiddenservicedir;
    logger.info("Set the hidden servide directory to: " + hiddenServiceDirectory);
  }


  /**
   * Returns the PTP working directory as specified.
   *
   * @return The PTP working directory.
   */
  public String getWorkingDirectory() {
    return workingDirectory;
  }

  /**
   * Returns the default Tor hidden service identifier.
   *
   * @return The default Tor hidden service identifier.
   */
  public String getDefaultIdentifier() {
    return defaultIdentifier;
  }

  /**
   * Returns the Tor hidden service directory as specified.
   *
   * @return The Tor hidden service directory.
   */
  public String getHiddenServiceDirectory() {
    return hiddenServiceDirectory;
  }

  /**
   * Returns the Tor hidden service port as specified.
   *
   * @return The Tor hidden service port.
   */
  public int getHiddenServicePort() {
    return hiddenServicePort;
  }

  /**
   * Returns the Tor bootstrap timeout (in milliseconds).
   *
   * @return The Tor bootstrap timeout.
   */
  public int getTorBootstrapTimeout() {
    return bootstrapTimeout;
  }

  /**
   * Returns the bytes needed by the Tor authentication message.
   *
   * @return The Tor authentication bytes.
   * @see <a href="https://gitweb.torproject.org/torspec.git/tree/control-spec.txt"> https://gitweb.
   *      torproject.org/torspec.git/tree/control-spec.txt</a>
   */
  public byte[] getAuthenticationBytes() {
    return authenticationBytes;
  }

  /**
   * Returns the Tor control socket port as specified.
   *
   * @return The Tor control sockert port.
   */
  public int getTorControlPort() {
    return torControlPort;
  }

  /**
   * Returns the Tor SOCKS proxy port as specified.
   *
   * @return The Tor control sockert port.
   */
  public int getTorSOCKSProxyPort() {
    return torSocksProxyPort;
  }

  /**
   * Returns the socket connection timeout (in milliseconds) when connecting to a hidden service
   * identifier.
   *
   * @return The socket connection timeout.
   */
  public int getSocketTimeout() {
    return socketTimeout;
  }

  /**
   * Returns the interval (in milliseconds) at which open sockets are polled for incoming data.
   *
   * @return The socket read poll interval.
   */
  public int getSocketReceivePoll() {
    return receivePoll;
  }

  /**
   * Returns the socket TTL (in milliseconds) of a connection to a hidden service identifier.
   *
   * @return The socket TTL of a connection to a hidden service identifier.
   */
  public int getSocketTTL() {
    return socketTtl;
  }

  /**
   * Returns the interval (in milliseconds) at which the TTL of all sockets is checked.
   *
   * @return The interval at which the TTL of all sockets is checked.
   */
  public int getTTLPoll() {
    return ttlPoll;
  }
}
