package edu.kit.tm.ptp;

import java.io.File;
import java.util.logging.Logger;

import edu.kit.tm.ptp.utility.Constants;


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
  private String loggerConfiguration = "config/logger.ini";
  /** The port on which the hidden service should be available. */
  private int hiddenServicePort = 8081;
  /** The authentication bytes needed by a control connection to Tor. */
  private byte[] authenticationBytes = new byte[0];
  /** The interval (in milliseconds) at each the TTL of all sockets is checked. */
  private int timerUpdateInterval = 1000;

  /** The path of the working directory. */
  private String workingDirectory;
  private String hiddenServicesDirectory;

  /** The port number of the Tor control socket. */
  private int torControlPort;
  /** The port number of the Tor SOCKS proxy. */
  private int torSocksProxyPort;
  private int isAliveTimeout = 30 * 1000;
  private int isAliveSendTimeout = 20 * 1000;
  private int connectRetryInterval = DEFAULT_CONNECTRETRYINTERVAL;
  private int messageSendRetryInterval = DEFAULT_MESSAGESENDRETRYINTERVAL;

  public static final int DEFAULT_MESSAGESENDRETRYINTERVAL = 5 * 1000;
  public static final int DEFAULT_CONNECTRETRYINTERVAL = 30 * 1000;

  protected Configuration() {

  }


  /**
   * Returns a formatted list of all configuration values.
   *
   * @return A string containing formatted configuration values.
   * @see Object
   */
  @Override
  public synchronized String toString() {
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
  public synchronized void setLoggerConfiguration(String loggerConfiguration) {
    if (loggerConfiguration == null) {
      throw new NullPointerException();
    }

    this.loggerConfiguration = loggerConfiguration;
    
    if (logger == null) {
      logger = Logger.getLogger(Configuration.class.getName());
    }
  }

  public synchronized void setIsAliveValues(int isAliveTimeout, int isAliveSendTimeout) {
    if (isAliveTimeout < 0 || isAliveSendTimeout < 0 || isAliveSendTimeout >= isAliveTimeout) {
      throw new IllegalArgumentException();
    }

    this.isAliveTimeout = isAliveTimeout;
    this.isAliveSendTimeout = isAliveSendTimeout;
  }

  public synchronized void setTimerUpdateInterval(int timerUpdateInterval) {
    if (timerUpdateInterval < 0) {
      throw new IllegalArgumentException();
    }

    this.timerUpdateInterval = timerUpdateInterval;
  }


  public synchronized void setHiddenServicePort(int hiddenServicePort) {
    portValid(hiddenServicePort);

    this.hiddenServicePort = hiddenServicePort;
  }


  public synchronized void setAuthenticationBytes(byte[] authenticationBytes) {
    if (authenticationBytes == null) {
      throw new NullPointerException();
    }

    this.authenticationBytes = (byte[]) authenticationBytes.clone();
  }

  public synchronized void setWorkingDirectory(String workingDirectory) {
    this.workingDirectory = workingDirectory;
  }

  public synchronized void setHiddenServicesDirectory(String hiddenServicesDirectory) {
    this.hiddenServicesDirectory = hiddenServicesDirectory;
  }


  public synchronized void setTorControlPort(int torControlPort) {
    portValid(torControlPort);

    this.torControlPort = torControlPort;
  }
  
  public synchronized void setTorSocksProxyPort(int torSocksProxyPort) {
    portValid(torSocksProxyPort);

    this.torSocksProxyPort = torSocksProxyPort;
  }

  public synchronized void setConnectRetryInterval(int connectRetryInterval) {
    if (connectRetryInterval < 0) {
      throw new IllegalArgumentException();
    }

    this.connectRetryInterval = connectRetryInterval;
  }

  public synchronized  void setMessageSendRetryInterval(int messageSendRetryInterval) {
    if (messageSendRetryInterval < 0) {
      throw new IllegalArgumentException();
    }

    this.messageSendRetryInterval = messageSendRetryInterval;
  }

  /**
   * Returns the PTP working directory.
   */
  public synchronized String getWorkingDirectory() {
    return workingDirectory;
  }

  /**
   * Returns the Tor hidden service directory.
   */
  public synchronized String getHiddenServicesDirectory() {
    return hiddenServicesDirectory;
  }

  /**
   * Returns the Tor hidden service port.
   */
  public synchronized int getHiddenServicePort() {
    return hiddenServicePort;
  }

  /**
   * Returns the bytes to authenticate a Tor control connection.
   *
   * @see <a href="https://gitweb.torproject.org/torspec.git/tree/control-spec.txt"> https://gitweb.
   *      torproject.org/torspec.git/tree/control-spec.txt</a>
   */
  public synchronized byte[] getAuthenticationBytes() {
    return (byte[]) authenticationBytes.clone();
  }

  /**
   * Returns the Tor control socket port.
   */
  public synchronized int getTorControlPort() {
    return torControlPort;
  }

  /**
   * Returns the Tor SOCKS proxy port.
   */
  public synchronized int getTorSOCKSProxyPort() {
    return torSocksProxyPort;
  }

  /**
   * Returns the interval (in milliseconds) at which timers are updated.
   */
  public synchronized int getTimerUpdateInterval() {
    return timerUpdateInterval;
  }


  /**
   * Returns the time (in milliseconds) to wait for a regular message to be sent
   * before an IsAliveMessage is sent as a response to a received message.
   */
  public synchronized int getIsAliveSendTimeout() {
    return isAliveSendTimeout;
  }

  /**
   * Returns the time (in milliseconds) the sender of a message waits for a response
   * before closing the connection.
   */
  public synchronized int getIsAliveTimeout() {
    return isAliveTimeout;
  }


  /**
   * Returns the time (in milliseconds) to wait between two consecutive connection attempts.
   */
  public synchronized int getConnectRetryInterval() {
    return connectRetryInterval;
  }

  /**
   * Returns the time (in milliseconds) to wait before retrying to send messages.
   */
  public synchronized int getMessageSendRetryInterval() {
    return messageSendRetryInterval;
  }

  private void portValid(int port) {
    if (port < 0 || port > Constants.maxport) {
      throw new IllegalArgumentException();
    }
  }
}
