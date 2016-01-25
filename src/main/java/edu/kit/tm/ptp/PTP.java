package edu.kit.tm.ptp;

import edu.kit.tm.ptp.connection.ConnectionManager;
import edu.kit.tm.ptp.raw.Configuration;
import edu.kit.tm.ptp.raw.ExpireListener;
import edu.kit.tm.ptp.raw.TorManager;
import edu.kit.tm.ptp.raw.connection.TTLManager;
import edu.kit.tm.ptp.serialization.ByteArrayMessage;
import edu.kit.tm.ptp.serialization.Serializer;
import edu.kit.tm.ptp.utility.Constants;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Wrapper class for the PeerTorPeer raw API. Provides the following on top of the raw API: *
 * automatic socket management * sets configuration parameters * uses a shared Tor instance
 *
 * @author Simeon Andreev
 *
 */
public class PTP implements ReceiveListener {
  /** The logger for this class. */
  private final Logger logger;
  /** The configuration of the client. */
  private final Configuration config;
  /** The Tor process manager. */
  private final TorManager tor;
  /** The manager that closes sockets when their TTL expires. */
  private final TTLManager ttlManager;
  /** A dummy sending listener to use when no listener is specified upon message sending. */
  private ReceiveListener receiveListener = new ReceiveListenerAdapter();

  private final HiddenServiceManager hiddenServiceManager;
  private final ConnectionManager connectionManager;
  private int hiddenServicePort;
  private final Serializer serializer = new Serializer();
  private ListenerContainer listeners = new ListenerContainer();

  /**
   * Constructor method. Creates an API wrapper which manages a Tor process.
   *
   * @throws IOException Propagates any IOException thrown by the construction of the raw API, the
   *         configuration, or the Tor process manager.
   *
   * @see Client
   * @see Configuration
   * @see TorManager
   */
  public PTP() throws IOException {
    this(null);
  }

  /**
   * Constructor method. Creates an API wrapper which manages a Tor process.
   *
   * @param directory The name of the hidden service to reuse. May be null to indicate no specific
   *        reuse request.
   * @throws IOException Propagates any IOException thrown by the construction of the raw API, the
   *         configuration, or the Tor process manager.
   *
   * @see Client
   * @see Configuration
   * @see TorManager
   */
  public PTP(String directory) throws IOException {
    addShutdownHook();
    // Read the configuration.
    config = new Configuration(Constants.configfile);
    // Create the logger after the configuration sets the logger properties file.
    logger = Logger.getLogger(Constants.ptplogger);

    // Create the Tor process manager and start the Tor process.
    tor = new TorManager();
    // Start the Tor process.
    tor.start();

    // Wait until the Tor bootstrapping is complete.
    final long start = System.currentTimeMillis();
    final long timeout = config.getTorBootstrapTimeout();

    logger.log(Level.INFO, "Waiting for Tors bootstrapping to finish.");
    while (!tor.ready() && tor.running() && System.currentTimeMillis() - start < timeout) {
      try {
        Thread.sleep(250);
      } catch (InterruptedException e) {
        // Waiting was interrupted. Do nothing.
      }
    }

    // Check if Tor is not running.
    if (!tor.running()) {
      throw new IOException("Starting Tor failed!");
    }

    // Check if we reached the timeout without a finished boostrapping.
    if (!tor.ready()) {
      tor.killtor();
      throw new IOException("Tor bootstrapping timeout expired!");
    }

    // Set the control ports.
    config.setTorConfiguration(tor.directory(), tor.controlport(), tor.socksport());

    connectionManager = new ConnectionManager(Constants.localhost, config.getTorSOCKSProxyPort(),
        config.getHiddenServicePort());
    connectionManager.setSerializer(serializer);
    connectionManager.setSendListener(new SendListenerAdapter());
    connectionManager.setReceiveListener(this);
    connectionManager.start();
    hiddenServicePort = connectionManager.startBindServer();

    hiddenServiceManager = new HiddenServiceManager(config, directory, hiddenServicePort);

    // Create and start the manager with the given TTL.
    ttlManager = new TTLManager(getTTLManagerListener(), config.getTTLPoll());
    ttlManager.start();
  }

  /**
   * Constructor method. Creates an API wrapper which uses a Tor process running outside the API.
   *
   * @param workingDirectory The working directory of the Tor process.
   * @param controlPort The control port of the Tor process.
   * @param socksPort The SOCKS port of the Tor process.
   * @throws IOException Propagates any IOException thrown by the construction of the raw API, the
   *         configuration, or the Tor process manager.
   *
   * @see Client
   * @see Configuration
   */
  public PTP(String workingDirectory, int controlPort, int socksPort) throws IOException {
    this(workingDirectory, controlPort, socksPort, Constants.anyport, null);
  }

  /**
   * Constructor method. Creates an API wrapper which uses a Tor process running outside the API.
   *
   * @param workingDirectory The working directory of the Tor process.
   * @param controlPort The control port of the Tor process.
   * @param socksPort The SOCKS port of the Tor process.
   * @param localPort The port on which the local hidden service should run.
   * @param directory The name of the hidden service to reuse. May be null to indicate no specific
   *        reuse request.
   * @throws IOException Propagates any IOException thrown by the construction of the raw API, the
   *         configuration, or the Tor process manager.
   *
   * @see Client
   * @see Configuration
   */
  public PTP(String workingDirectory, int controlPort, int socksPort, int localPort,
      String directory) throws IOException {
    addShutdownHook();
    // Read the configuration.
    config = new Configuration(workingDirectory + "/" + Constants.configfile);
    // Create the logger after the configuration sets the logger properties file.
    logger = Logger.getLogger(Constants.ptplogger);

    // We will use an already running Tor instance, instead of managing one.
    tor = null;

    // Set the control ports.
    config.setTorConfiguration(workingDirectory, controlPort, socksPort);

    connectionManager = new ConnectionManager(Constants.localhost, config.getTorSOCKSProxyPort(),
        config.getHiddenServicePort());
    connectionManager.setSerializer(serializer);
    connectionManager.setSendListener(new SendListenerAdapter());
    connectionManager.setReceiveListener(this);
    connectionManager.start();
    hiddenServicePort = connectionManager.startBindServer();

    hiddenServiceManager = new HiddenServiceManager(config, directory, hiddenServicePort);

    // Create and start the manager with the given TTL.
    ttlManager = new TTLManager(getTTLManagerListener(), config.getTTLPoll());
    ttlManager.start();
  }

  private void addShutdownHook() {
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        logger.log(Level.INFO, "Shutdown hook called");
        exit();
      }
    });
  }


  /**
   * Returns the currently used API configuration.
   *
   * @return The currently used API configuration.
   */
  public Configuration getConfiguration() {
    return config;
  }

  /**
   * Returns the currently used hidden service identifier.
   *
   * @return The hidden service identifier of the used/created hidden service.
   *
   * @see Client
   */
  public Identifier getIdentifier() {
    return hiddenServiceManager.getHiddenServiceIdentifier();
  }

  /**
   * Creates a hidden service, if possible reuses the hidden service indicated at API wrapper
   * creation.
   *
   * @throws IOException Propagates any IOException the API received while creating the hidden
   *         service.
   *
   * @see Client
   */
  public void reuseHiddenService() throws IOException {
    hiddenServiceManager.reuseHiddenService();
    connectionManager.setLocalIdentifier(getIdentifier());
  }

  /**
   * Creates a fresh hidden service.
   *
   * @throws IOException Propagates any IOException the API received while creating the hidden
   *         service.
   *
   * @see Client
   */
  public void createHiddenService() throws IOException {
    // Create a fresh hidden service identifier.
    hiddenServiceManager.createHiddenService();
    connectionManager.setLocalIdentifier(getIdentifier());
  }

  public long sendMessage(byte[] data, Identifier destination, long timeout) {
    ByteArrayMessage msg = new ByteArrayMessage(data);
    return sendMessage(msg, destination, timeout);
  }

  public long sendMessage(byte[] data, Identifier destination) {
    // TODO set appropriate default timeout
    return sendMessage(data, destination, -1);
  }

  public long sendMessage(Object message, Identifier destination) {
    // TODO set appropriate default timeout
    return sendMessage(message, destination, -1);
  }

  public long sendMessage(Object message, Identifier destination, long timeout) {
    byte[] data = serializer.serialize(message);
    return connectionManager.send(data, destination, timeout);
  }

  public <T> void registerMessage(Class<T> type, MessageReceivedListener<T> listener) {
    serializer.registerClass(type);
    listeners.putListener(type, listener);
  }

  public void setReceiveListener(ReceiveListener listener) {
    this.receiveListener = listener;
  }

  public void setSendListener(SendListener listener) {
    connectionManager.setSendListener(listener);
  }

  /**
   * Returns the local port on which the local hidden service is available.
   *
   * @return The local port on which the local hidden service is available.
   *
   * @see Client
   */
  public int getLocalPort() {
    // Get the local port from the client.
    return hiddenServicePort;
  }

  public void deleteHiddenService() {
    try {
      hiddenServiceManager.deleteHiddenService();
    } catch (IOException ioe) {
      logger.log(Level.WARNING,
          "Received IOException while deleting the hidden service directory: " + ioe.getMessage());
    }
  }

  /**
   * Closes the local socket and any open connections. Stops the socket TTL manager and the Tor
   * process manager.
   *
   * @see Client
   */
  public void exit() {
    if (connectionManager != null) {
      connectionManager.stop();
    }

    if (ttlManager != null) {
      ttlManager.stop();
    }

    // Close the Tor process manager.
    if (tor != null) {
      tor.stop();
    }

    if (hiddenServiceManager != null) {
      hiddenServiceManager.close();
    }
  }


  /**
   * Returns a manager listener that will close socket connections with expired TTL.
   *
   * @return The manager listener which closes sockets with no TTL left.
   */
  private ExpireListener getTTLManagerListener() {
    return new ExpireListener() {

      /**
       * Set the listener to disconnect connections with expired TTL.
       *
       * @param identifier The identifier with the expired socket connection.
       *
       * @see TTLManager.Listener
       */
      @Override
      public void expired(Identifier identifier) throws IOException {
        connectionManager.disconnect(identifier);
      }

    };
  }

  @Override
  public void messageReceived(byte[] data, Identifier source) {
    Object obj;
    try {
      obj = serializer.deserialize(data);

      if (obj instanceof ByteArrayMessage) {
        ByteArrayMessage message = (ByteArrayMessage) obj;
        receiveListener.messageReceived(message.getData(), source);
      } else {
        listeners.callListener(obj, source);
      }
    } catch (IOException e) {
      logger.log(Level.WARNING, "Error occurred while deserializing data: " + e.getMessage());
    }
  }
}
