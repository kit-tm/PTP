package edu.kit.tm.ptp;

import edu.kit.tm.ptp.connection.ConnectionManager;
import edu.kit.tm.ptp.connection.ExpireListener;
import edu.kit.tm.ptp.connection.TTLManager;
import edu.kit.tm.ptp.hiddenservice.HiddenServiceManager;
import edu.kit.tm.ptp.serialization.ByteArrayMessage;
import edu.kit.tm.ptp.serialization.Serializer;
import edu.kit.tm.ptp.utility.Constants;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Provides the PTP API.
 *
 * @author Simeon Andreev
 * @author Timon Hackenjos
 *
 */
public class PTP implements ReceiveListener {
  /** The logger for this class. */
  private Logger logger;
  /** The configuration of the client. */
  private Configuration config;
  private final ConfigurationFileReader configReader;
  /** The Tor process manager. */
  private TorManager tor;
  /** The manager that closes sockets when their TTL expires. */
  private TTLManager ttlManager;
  /** A dummy sending listener to use when no listener is specified upon message sending. */
  private ReceiveListener receiveListener = new ReceiveListenerAdapter();

  private HiddenServiceManager hiddenServiceManager;
  private ConnectionManager connectionManager;
  private int hiddenServicePort;
  private final Serializer serializer = new Serializer();
  private final ListenerContainer listeners = new ListenerContainer();
  private boolean initialized = false;
  private String hiddenServiceDirectory;
  private String workingDirectory;
  private int controlPort;
  private int socksPort;
  private final boolean usePTPTor;

  /**
   * Constructs a new PTP object. Manages a own Tor process.
   *
   * @throws IOException If an error occurs.
   */
  public PTP() {
    this(null);
  }

  /**
   * Constructs a new PTP object using the supplied hidden service directory name. Manages an own
   * Tor process.
   *
   * @param directory The name of the hidden service directory.
   * @throws IOException If an error occurs.
   */
  public PTP(String directory) {
    this(directory, true);
    hiddenServicePort = Constants.anyport;
  }

  /**
   * Constructs a new PTP object. Uses an already running Tor process.
   *
   * @param workingDirectory The working directory of the Tor process.
   * @param controlPort The control port of the Tor process.
   * @param socksPort The SOCKS port of the Tor process.
   * @throws IOException If an error occurs.
   */
  public PTP(String workingDirectory, int controlPort, int socksPort) {
    this(workingDirectory, controlPort, socksPort, Constants.anyport, null);
  }

  /**
   * Constructs a new PTP object. Uses an already running Tor process.
   *
   * @param workingDirectory The working directory of the Tor process.
   * @param controlPort The control port of the Tor process.
   * @param socksPort The SOCKS port of the Tor process.
   * @param localPort The port on which the local hidden service should run.
   * @param directory The name of the hidden service directory.
   * @throws IOException If an error occurs.
   *
   */
  public PTP(String workingDirectory, int controlPort, int socksPort, int localPort,
      String directory) {
    this(directory, false);

    hiddenServicePort = localPort;

    this.workingDirectory = workingDirectory;
    this.controlPort = controlPort;
    this.socksPort = socksPort;
  }

  private PTP(String directory, boolean usePTPTor) {
    configReader = new ConfigurationFileReader(Constants.configfile);

    this.hiddenServiceDirectory = directory;
    this.usePTPTor = usePTPTor;
  }

  /**
   * Initializes the PTP object. 
   * Reads the configuration file and starts Tor if PTP manages the Tor process.
   * Sets up necessary managers.
   * 
   * @throws IOException If starting Tor or starting another service fails.
   */
  public void init() throws IOException {
    addShutdownHook();

    // read the configuration file
    config = configReader.readFromFile();

    // Create the logger after the configuration sets the logger properties file.
    logger = Logger.getLogger(PTP.class.getName());

    if (usePTPTor) {
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

      config.setTorConfiguration(tor.directory(), tor.controlport(), tor.socksport());
    } else {
      config.setTorConfiguration(workingDirectory, controlPort, socksPort);
    }

    connectionManager = new ConnectionManager(Constants.localhost, config.getTorSOCKSProxyPort(),
        config.getHiddenServicePort());
    connectionManager.setSerializer(serializer);
    connectionManager.setSendListener(new SendListenerAdapter());
    connectionManager.setReceiveListener(this);

    connectionManager.start();
    hiddenServicePort = connectionManager.startBindServer(hiddenServicePort);
    hiddenServiceManager =
        new HiddenServiceManager(config, hiddenServiceDirectory, hiddenServicePort);

    // Create the manager with the given TTL.
    ttlManager = new TTLManager(getTTLManagerListener(), config.getTTLPoll());
    // Start the manager with the given TTL.
    ttlManager.start();
    initialized = true;
  }

  /**
   * Returns the currently used API configuration.
   */
  public Configuration getConfiguration() {
    return config;
  }

  /**
   * Returns the currently used hidden service identifier.
   */
  public Identifier getIdentifier() {
    return hiddenServiceManager.getHiddenServiceIdentifier();
  }

  /**
   * Reuses a hidden service or creates a new one if no hidden service to reuse exists.
   */
  public void reuseHiddenService() throws IOException {
    if (!initialized) {
      throw new IllegalStateException();
    }

    hiddenServiceManager.reuseHiddenService();
    connectionManager.setLocalIdentifier(getIdentifier());
  }

  /**
   * Creates a fresh hidden service.
   */
  public void createHiddenService() throws IOException {
    if (!initialized) {
      throw new IllegalStateException();
    }

    // Create a fresh hidden service identifier.
    hiddenServiceManager.createHiddenService();
    connectionManager.setLocalIdentifier(getIdentifier());
  }

  /**
   * Sends bytes to the supplied destination.
   * 
   * @param data The data to send.
   * @param destination The hidden service identifier of the destination.
   * @param timeout How long to wait for a successful transmission.
   * @return Identifier of the message.
   */
  public long sendMessage(byte[] data, Identifier destination, long timeout) {
    ByteArrayMessage msg = new ByteArrayMessage(data);
    return sendMessage(msg, destination, timeout);
  }

  /**
   * Sends bytes to the supplied destination.
   * 
   * @param data The data to send.
   * @param destination The hidden service identifier of the destination.
   */
  public long sendMessage(byte[] data, Identifier destination) {
    // TODO set appropriate default timeout
    return sendMessage(data, destination, -1);
  }

  /**
   * Send an object of a previously registered class to the supplied destination.
   * 
   * @param message The object to send.
   * @param destination The hidden service identifier of the destination.
   * @return Identifier of the message.
   * @see registerMessage
   */
  public long sendMessage(Object message, Identifier destination) {
    // TODO set appropriate default timeout
    return sendMessage(message, destination, -1);
  }

  /**
   * Send an object of a previously registered class to the supplied destination.
   * 
   * @param message The object to send.
   * @param destination The hidden service identifier of the destination.
   * @param timeout How long to wait for a successful transmission.
   * @return Identifier of the message.
   * @see registerMessage
   */
  public long sendMessage(Object message, Identifier destination, long timeout) {
    if (!initialized) {
      throw new IllegalStateException();
    }

    byte[] data = serializer.serialize(message);
    return connectionManager.send(data, destination, timeout);
  }

  /**
   * Register class to be able to send and receive objects of the class as message and registers an
   * appropriate listener.
   * 
   * @param type The class to register.
   * @param listener Listener to be informed about received objects of the class.
   */
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
   * Returns the local port on which the local hidden service is listening.
   */
  public int getLocalPort() {
    return hiddenServicePort;
  }

  /**
   * Delete the currently used hidden service directory.
   */
  public void deleteHiddenService() {
    if (!initialized) {
      throw new IllegalStateException();
    }

    try {
      hiddenServiceManager.deleteHiddenService();
    } catch (IOException ioe) {
      logger.log(Level.WARNING,
          "Received IOException while deleting the hidden service directory: " + ioe.getMessage());
    }
  }

  /**
   * Closes the local hidden service and any open connections. Stops the socket TTL manager and the
   * Tor process manager. Does nothing if exit has already been called before.
   */
  public void exit() {
    if (!initialized) {
      return;
    }

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


  /**
   * Returns a manager listener that will close socket connections with expired TTL.
   */
  private ExpireListener getTTLManagerListener() {
    return new ExpireListener() {

      /**
       * Set the listener to disconnect connections with expired TTL.
       *
       * @param identifier The identifier with:00 the expired socket connection.
       *
       * @see TTLManager.Listener
       */
      @Override
      public void expired(Identifier identifier) throws IOException {
        connectionManager.disconnect(identifier);
      }

    };
  }

  private void addShutdownHook() {
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        if (initialized) {
          logger.log(Level.INFO, "Shutdown hook called");
          exit();
        }
      }
    });
  }
}
