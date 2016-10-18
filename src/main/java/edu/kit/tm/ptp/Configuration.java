package edu.kit.tm.ptp;

import java.util.logging.Logger;


/**
 * Holds the PeerTorPeer (PTP) configuration.
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
  /** The port on which the hidden service should be available. */
  private int hiddenServicePort;
  /** The authentication bytes needed by a control connection to Tor. */
  private byte[] authenticationBytes;
  /** The interval (in milliseconds) at each the TTL of all sockets is checked. */
  private int timerUpdateInterval;

  /** The path of the working directory. */
  private String workingDirectory;
  private String hiddenServicesDirectory;

  /** The port number of the Tor control socket. */
  private int torControlPort;
  /** The port number of the Tor SOCKS proxy. */
  private int torSocksProxyPort;
  private int isAliveTimeout;
  private int isAliveSendTimeout;


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

    sb.append("\thidden services directory = ");
    sb.append(hiddenServicesDirectory);
    sb.append("\n");

    sb.append("\thidden service port number = ");
    sb.append(hiddenServicePort);
    sb.append("\n");

    sb.append("\tTor control port number = ");
    sb.append(torControlPort);
    sb.append("\n");

    sb.append("\tTimer update interval = ");
    sb.append(timerUpdateInterval);
    sb.append("\n");

    sb.append("\tlogger configuration file = ");
    sb.append(loggerConfiguration);
    sb.append("\n");

    sb.append("\tIs alive timeout = ");
    sb.append(isAliveTimeout);
    sb.append("\n");

    sb.append("\tIs alive send timeout = ");
    sb.append(isAliveSendTimeout);
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

  public void setIsAliveTimeout(int isAliveTimeout) {
    this.isAliveTimeout = isAliveTimeout;
  }

  public void setIsAliveSendTimeout(int isAliveSendTimeout) {
    this.isAliveSendTimeout = isAliveSendTimeout;
  }

  public void setTimerUpdateInterval(int timerUpdateInterval) {
    this.timerUpdateInterval = timerUpdateInterval;
  }


  public void setHiddenServicePort(int hiddenServicePort) {
    this.hiddenServicePort = hiddenServicePort;
  }


  public void setAuthenticationBytes(byte[] authenticationBytes) {
    this.authenticationBytes = (byte[]) authenticationBytes.clone();
  }

  public void setWorkingDirectory(String workingDirectory) {
    this.workingDirectory = workingDirectory;
  }

  public void setHiddenServicesDirectory(String hiddenServicesDirectory) {
    this.hiddenServicesDirectory = hiddenServicesDirectory;
  }


  public void setTorControlPort(int torControlPort) {
    this.torControlPort = torControlPort;
  }
  
  public void setTorSocksProxyPort(int torSocksProxyPort) {
    this.torSocksProxyPort = torSocksProxyPort;
  }

  /**
   * Returns the PTP working directory.
   */
  public String getWorkingDirectory() {
    return workingDirectory;
  }

  /**
   * Returns the Tor hidden service directory.
   */
  public String getHiddenServicesDirectory() {
    return hiddenServicesDirectory;
  }

  /**
   * Returns the Tor hidden service port.
   */
  public int getHiddenServicePort() {
    return hiddenServicePort;
  }

  /**
   * Returns the bytes to authenticate a Tor control connection.
   *
   * @see <a href="https://gitweb.torproject.org/torspec.git/tree/control-spec.txt"> https://gitweb.
   *      torproject.org/torspec.git/tree/control-spec.txt</a>
   */
  public byte[] getAuthenticationBytes() {
    return (byte[]) authenticationBytes.clone();
  }

  /**
   * Returns the Tor control socket port.
   */
  public int getTorControlPort() {
    return torControlPort;
  }

  /**
   * Returns the Tor SOCKS proxy port.
   */
  public int getTorSOCKSProxyPort() {
    return torSocksProxyPort;
  }

  /**
   * Returns the interval (in milliseconds) at which timers are updated.
   */
  public int getTimerUpdateInterval() {
    return timerUpdateInterval;
  }


  /**
   * Returns the time (in milliseconds) to wait for a regular message to be sent
   * before an IsAliveMessage is sent as a response to a received message.
   */
  public int getIsAliveSendTimeout() {
    return isAliveSendTimeout;
  }

  /**
   * Returns the time (in milliseconds) the sender of a message waits for a response
   * before closing the connection.
   */
  public int getIsAliveTimeout() {
    return isAliveTimeout;
  }

}
