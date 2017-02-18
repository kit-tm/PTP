package edu.kit.tm.ptp;

import edu.kit.tm.ptp.auth.AuthenticatorFactory;
import edu.kit.tm.ptp.auth.PublicKeyAuthenticatorFactory;
import edu.kit.tm.ptp.connection.ConnectionManager;
import edu.kit.tm.ptp.hiddenservice.HiddenServiceManager;
import edu.kit.tm.ptp.serialization.ByteArrayMessage;
import edu.kit.tm.ptp.serialization.Serializer;
import edu.kit.tm.ptp.utility.Constants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Class that provides the PTP API. The method {@link #init() init()} needs to be called to to use
 * PTP. Before calling {@link #init() init()} only the following methods may be called:
 * {@link #setReceiveListener(ReceiveListener) setReceiveListener(ReceiveListener)},
 * {@link #setSendListener(SendListener) setSendListener(SendListener)},
 * {@link #setReceiveListener(Class, MessageReceivedListener) setReceiveListener(Class,
 * MessageReceivedListener)}, {@link #registerClass(Class) registerClass(Class)},
 * {@link #enableMessageQueue() enableMessageQueue()}, {@link #enableMessageQueue(Class)
 * enableMessageQueue(Class)}.
 *
 * @author Timon Hackenjos
 * @author Simeon Andreev
 *
 */
public class PTP {
  private final MessageQueueContainer messageTypes = new MessageQueueContainer();
  private final ConfigurationFileReader configReader;
  private final Serializer serializer;
  private final String hiddenServiceDirectoryName;
  private final boolean usePTPTor;
  private final Thread clientThread;
  private final boolean sharedTorProcess;
  private final ThreadGroup ptpGroup = new PTPThreadGroup(Constants.threadGroupName);

  /** The logger for this class. */
  private Logger logger;
  /** The configuration of the client. */
  private Configuration config = null;
  /** The Tor process manager. */
  private TorManager tor;
  private ReceiveListener receiveListener = null;
  private SendListener sendListener = new SendListenerAdapter();
  private HiddenServiceManager hiddenServiceManager;
  private String workingDirectory;
  private int controlPort;
  private IsAliveManager isAliveManager = null;

  private volatile boolean initialized = false;
  private volatile boolean closed = false;
  private volatile boolean queueMessages = false;
  private volatile int hiddenServicePort;

  protected ConnectionManager connectionManager;
  protected AuthenticatorFactory authFactory = new PublicKeyAuthenticatorFactory();

  /**
   * Constructs a new PTP object. Manages an own Tor process.
   */
  public PTP() {
    // Default constructor
    this(null, null);
  }

  /**
   * Constructs a new PTP object. Uses an already running Tor process.
   *
   * @param workingDirectory The directory to start PTP in.
   * @param controlPort The control port of the Tor process.
   */
  public PTP(String workingDirectory, int controlPort) {
    // Constructor using an external Tor process
    this(workingDirectory, controlPort, Constants.anyport, null);
  }

  /**
   * Constructs a new PTP object. Uses an already running Tor process.
   *
   * @param workingDirectory The directory to start PTP in.
   * @param controlPort The control port of the Tor process.
   * @param localPort The port on which the local hidden service should run.
   * @param hiddenServiceDirectoryName The name of the hidden service directory.
   *
   */
  public PTP(String workingDirectory, int controlPort, int localPort,
      String hiddenServiceDirectoryName) {
    // Constructor using an external Tor process
    this(workingDirectory, hiddenServiceDirectoryName, false, localPort, false, null);

    this.controlPort = controlPort;
  }

  /**
   * Constructs a new PTP object using the supplied hidden service directory name and working
   * directory. Manages an own Tor process.
   * Used by PTPAndroid.
   * 
   * @param workingDirectory The directory to start PTP in.
   * @param hiddenServiceDirectoryName The name of the directory of a hidden service to use.
   */
  public PTP(String workingDirectory, String hiddenServiceDirectoryName) {
    this(workingDirectory, hiddenServiceDirectoryName, false, null);
  }

  /**
   * Constructs a new PTP object. Manages an own Tor process.
   * Only used for testing.
   *
   * @param sharedTorProcess If true the Tor process will be shared between PTP instances in the
   *        same directory.
   */
  public PTP(boolean sharedTorProcess) {
    this(null, null, sharedTorProcess, null);
  }

  /**
   * Constructs a new PTP object. Manages an own Tor process.
   * Only used for testing.
   *
   * @param sharedTorProcess If true the Tor process will be shared between PTP instances in the
   *        same directory.
   * @param config An initial configuration object used instead of reading a configuration file.
   */
  protected PTP(boolean sharedTorProcess, Configuration config) {
    this(null, null, sharedTorProcess, config);
  }

  private PTP(String workingDirectory, String hiddenServiceDirectoryName, boolean sharedTorProcess,
             Configuration config) {
    this(workingDirectory, hiddenServiceDirectoryName, true, Constants.anyport,
        sharedTorProcess, config);
  }

  private PTP(String workingDirectory, String hiddenServiceDirectoryName,
      boolean usePTPTor, int hiddenServicePort, boolean sharedTorProcess, Configuration config) {
    configReader = new ConfigurationFileReader(
        (workingDirectory != null ? workingDirectory + File.separator : "") + Constants.configfile);

    this.workingDirectory = workingDirectory;
    this.hiddenServiceDirectoryName = hiddenServiceDirectoryName;
    this.usePTPTor = usePTPTor;
    this.hiddenServicePort = hiddenServicePort;
    this.sharedTorProcess = sharedTorProcess;

    this.serializer = new Serializer();
    this.config = config;
    serializer.registerClass(byte[].class);
    serializer.registerClass(ByteArrayMessage.class);

    clientThread = Thread.currentThread();
    messageTypes.addMessageQueue(byte[].class);
  }

  /**
   * Initializes the PTP object. Reads the configuration file and starts Tor if PTP manages the Tor
   * process.
   * 
   * @throws IOException If starting Tor fails.
   */
  public synchronized void init() throws IOException {
    if (initialized) {
      throw new IllegalStateException("PTP is already initialized.");
    }

    if (closed) {
      throw new IllegalStateException("PTP is already closed.");
    }

    addShutdownHook();

    if (config == null) {
      // read the configuration file
      config = configReader.readFromFile();
    }

    // Create the logger after the configuration sets the logger properties file.
    logger = Logger.getLogger(PTP.class.getName());

    if (workingDirectory == null) {
      String ptphome = System.getenv(Constants.ptphome);

      if (ptphome == null) {
        workingDirectory = Constants.ptphomedefault;
      } else {
        workingDirectory = ptphome;
      }
    }

    config.setWorkingDirectory(workingDirectory);
    config
        .setHiddenServicesDirectory(workingDirectory + File.separator + Constants.hiddenservicedir);

    if (usePTPTor) {
      if (sharedTorProcess) {
        tor = new SharedTorManager(workingDirectory, config);
      } else {
        tor = new TorManager(workingDirectory, config);
      }
    } else {
      tor = new TorManager(controlPort, config);
    }

    connectionManager = new ConnectionManager(config.getHiddenServicePort(),
        new PTPReceiveListener(), new PTPSendListener(), config, ptpGroup, authFactory);

    tor.addSOCKSProxyListener(new SOCKSProxyPortListener());
    tor.addSOCKSProxyListener(connectionManager);

    // Start the Tor process.
    if (!tor.startTor()) {
      throw new IOException("Failed to start Tor");
    }

    config.setTorControlPort(tor.getTorControlPort());

    connectionManager.start();
    hiddenServicePort = connectionManager.startBindServer(hiddenServicePort);
    hiddenServiceManager =
        new HiddenServiceManager(config, hiddenServiceDirectoryName, hiddenServicePort, tor);

    isAliveManager = new IsAliveManager(this, config);
    isAliveManager.start();

    initialized = true;
  }

  /**
   * Returns true if PTP was initialized successfully.
   */
  public synchronized boolean isInitialized() {
    return initialized;
  }

  /**
   * Returns the currently used API configuration.
   */
  public synchronized Configuration getConfiguration() {
    if (!initialized || closed) {
      throw new IllegalStateException();
    }

    return config;
  }

  /**
   * Returns the currently used hidden service identifier.
   */
  public synchronized Identifier getIdentifier() {
    if (!initialized || closed) {
      throw new IllegalStateException();
    }

    return hiddenServiceManager.getHiddenServiceIdentifier();
  }

  /**
   * Returns the directory of the currently used hidden service.
   */
  public synchronized String getHiddenServiceDirectory() {
    if (!initialized || closed) {
      throw new IllegalStateException();
    }

    return hiddenServiceManager.getHiddenServiceDirectory();
  }

  /**
   * Reuses a hidden service or creates a new one if no hidden service to reuse exists.
   */
  public synchronized void reuseHiddenService() throws IOException {
    if (!initialized || closed) {
      throw new IllegalStateException();
    }

    hiddenServiceManager.reuseHiddenService();
    connectionManager.setIdentity(hiddenServiceManager.getPrivateKeyFile(), getIdentifier());
  }

  /**
   * Creates a fresh hidden service.
   */
  public synchronized void createHiddenService() throws IOException {
    if (!initialized || closed) {
      throw new IllegalStateException();
    }

    // Create a fresh hidden service identifier.
    hiddenServiceManager.createHiddenService();
    connectionManager.setIdentity(hiddenServiceManager.getPrivateKeyFile(), getIdentifier());
  }



  /**
   * Sends bytes to the supplied destination.
   * 
   * @param data The data to send.
   * @param destination The hidden service identifier of the destination.
   * @param timeout How long to wait for a successful transmission.
   * @return Identifier of the message.
   */
  public synchronized long sendMessage(byte[] data, Identifier destination, long timeout) {
    if (data == null || destination == null) {
      throw new IllegalArgumentException();
    }

    ByteArrayMessage msg = new ByteArrayMessage(data);
    return sendMessage(msg, destination, timeout);
  }

  /**
   * Sends bytes to the supplied destination.
   * 
   * @param data The data to send.
   * @param destination The hidden service identifier of the destination.
   */
  public synchronized long sendMessage(byte[] data, Identifier destination) {
    if (data == null || destination == null) {
      throw new IllegalArgumentException();
    }

    return sendMessage(data, destination, -1);
  }

  /**
   * Send an object of a previously registered class to the supplied destination.
   * 
   * @param message The object to send.
   * @param destination The hidden service identifier of the destination.
   * @return Identifier of the message.
   * @see #enableMessageQueue(Class)
   */
  public synchronized long sendMessage(Object message, Identifier destination) {
    if (message == null || destination == null) {
      throw new IllegalArgumentException();
    }

    return sendMessage(message, destination, -1);
  }

  /**
   * Send an object of a previously registered class to the supplied destination.
   * 
   * @param message The object to send.
   * @param destination The hidden service identifier of the destination.
   * @param timeout How long to wait for a successful transmission.
   * @return Identifier of the message.
   * @see #enableMessageQueue(Class)
   */
  public synchronized long sendMessage(Object message, Identifier destination, long timeout) {
    if (!initialized || closed) {
      throw new IllegalStateException();
    }

    if (message == null || destination == null) {
      throw new IllegalArgumentException();
    }

    byte[] data = serializer.serialize(message);
    return connectionManager.send(data, destination, timeout);
  }

  /**
   * Register class to be able to send and receive instances of the class. Registering a class
   * several times has no effect.
   */
  public synchronized <T> void registerClass(Class<T> type) {
    if (closed) {
      throw new IllegalStateException();
    }
    serializer.registerClass(type);
  }

  /**
   * Sets a listener for a previously registered class.
   * 
   * @param type The class type of the objects to be informed about.
   * @param listener Listener to be informed about received objects.
   * @see #registerClass(Class)
   */
  public synchronized <T> void setReceiveListener(Class<T> type,
                                                  MessageReceivedListener<T> listener) {
    if (closed) {
      throw new IllegalStateException();
    }

    if (!serializer.isRegistered(type)) {
      throw new IllegalArgumentException("Class type hasn't been registered before");
    }

    messageTypes.putListener(type, listener);
  }

  /**
   * Sets the listener for received byte[] messages.
   * 
   * @param listener The listener to inform.
   */
  public synchronized void setReceiveListener(ReceiveListener listener) {
    if (closed) {
      throw new IllegalStateException();
    }

    this.receiveListener = listener;
  }

  /**
   * Enables queueing of objects of a previously registered type. Objects can be received using
   * {@link #getMessageQueue(Class) getMessageQueue(Class)}.
   * 
   * @param type The type of objects to queue.
   * @see #setReceiveListener(Class, MessageReceivedListener)
   */
  public synchronized <T> void enableMessageQueue(Class<T> type) {
    if (closed) {
      throw new IllegalStateException();
    }

    if (!serializer.isRegistered(type)) {
      throw new IllegalArgumentException("Class type hasn't been registered before");
    }
    messageTypes.addMessageQueue(type);
  }

  /**
   * Enables queueing of byte[] messages. Objects can be received using {@link #getMessageQueue()
   * getMessageQueue()}
   */
  public synchronized void enableMessageQueue() {
    if (closed) {
      throw new IllegalStateException();
    }

    this.queueMessages = true;
  }

  /**
   * Returns a IMessageQueue to poll received messages of the supplied type from.
   *
   * @see #enableMessageQueue(Class)
   */
  public synchronized <T> IMessageQueue<T> getMessageQueue(Class<T> type) {
    if (closed) {
      throw new IllegalStateException();
    }

    if (!serializer.isRegistered(type)) {
      throw new IllegalArgumentException("Class type hasn't been registered before");
    }

    if (!messageTypes.queueEnabled(type)) {
      throw new IllegalArgumentException("Queuing hasn't been enabled for the type");
    }

    return new MessageQueue<T>(type, messageTypes);
  }

  /**
   * Returns a IMessageQueue to poll received byte[] messages from.
   */
  public synchronized IMessageQueue<byte[]> getMessageQueue() {
    return getMessageQueue(byte[].class);
  }

  /**
   * Sets the listener to be informed about sent messages.
   * 
   * @param listener The lister to inform.
   */
  public synchronized void setSendListener(SendListener listener) {
    if (closed) {
      throw new IllegalStateException();
    }

    this.sendListener = listener;
  }

  /**
   * Returns the local port on which the local hidden service is listening.
   */
  public synchronized int getLocalPort() {
    if (!initialized || closed) {
      throw new IllegalStateException();
    }

    return hiddenServicePort;
  }

  /**
   * Delete the currently used hidden service directory. The method is only allowed to be called
   * after {@link #exit() exit} has been called.
   */
  public synchronized void deleteHiddenService() {
    if (!(initialized && closed)) {
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
   * Stops PTP. Only the method {@link #deleteHiddenService() deleteHiddenService()} may be called
   * afterwards. Calling this method several times has no effect.
   */
  public synchronized void exit() {
    if (closed) {
      return;
    }

    if (connectionManager != null) {
      connectionManager.stop();
    }

    if (isAliveManager != null) {
      isAliveManager.stop();
    }

    // Close the Tor process manager.
    if (tor != null) {
      tor.stopTor();
    }

    if (hiddenServiceManager != null) {
      hiddenServiceManager.close();
    }

    closed = true;
  }

  /**
   * Gives a hint to PTP if a network connection is currently up and internet access
   * should be working.
   *
   * @param enable True if network connectivity is up.
   */
  public synchronized void changeNetwork(boolean enable) {
    if (!initialized || closed) {
      throw new IllegalStateException();
    }

    tor.changeNetwork(enable);
  }

  protected synchronized void closeConnections(Identifier destination) {
    if (!initialized || closed) {
      throw new IllegalStateException();
    }

    tor.closeCircuits(destination);
  }
  
  protected synchronized void sendIsAlive(Identifier destination, long timeout) {
    connectionManager.send(new byte[0], destination, timeout, false);
  }

  private class PTPReceiveListener implements ReceiveListener {
    @Override
    public void messageReceived(byte[] data, Identifier source) {
      synchronized (PTP.this) {
        Object obj;
        boolean isAliveMsg = data.length == 0;
        try {
          isAliveManager.messageReceived(source, isAliveMsg);

          if (isAliveMsg) {
            return;
          }

          obj = serializer.deserialize(data);

          if (obj instanceof ByteArrayMessage) {
            ByteArrayMessage message = (ByteArrayMessage) obj;

            if (receiveListener != null) {
              receiveListener.messageReceived(message.getData(), source);
            }

            if (queueMessages) {
              messageTypes.addMessageToQueue(message.getData(), source, System.currentTimeMillis());
            }

            if (receiveListener == null && !queueMessages) {
              logger.log(Level.WARNING,
                  "Dropping received message because no receive listener ist set.");
            }
          } else {
            if (messageTypes.hasListener(obj)) {
              messageTypes.callReceiveListener(obj, source);
            }
            if (messageTypes.hasQueue(obj)) {
              messageTypes.addMessageToQueue(obj, source, System.currentTimeMillis());
            }

            if (!messageTypes.hasListener(obj) && !messageTypes.hasQueue(obj)) {
              logger.log(Level.WARNING,
                  "Received message of unregistered type with length " + data.length);
            }
          }
        } catch (IOException e) {
          logger.log(Level.WARNING, "Error occurred while deserializing data: " + e.getMessage());
        }
      }
    }
  }

  private class PTPSendListener implements SendListener {
    @Override
    public void messageSent(long id, Identifier destination, State state) {
      synchronized (PTP.this) {
        // PTP doesn't allow to send messages before initialization is finished
        if (!initialized) {
          throw new IllegalStateException();
        }

        if (state == State.SUCCESS) {
          isAliveManager.messageSent(destination);
        }

        sendListener.messageSent(id, destination, state);
      }
    }
  }

  private class SOCKSProxyPortListener implements  TorManager.SOCKSProxyListener {

    @Override
    public void updateSOCKSProxy(String socksHost, int socksProxyPort) {
      config.setTorSocksProxyPort(socksProxyPort);
    }
  }

  private class PTPThreadGroup extends ThreadGroup {
    public PTPThreadGroup(String name) {
      super(name);
    }

    public PTPThreadGroup(ThreadGroup parent, String name) {
      super(parent, name);
    }

    @Override
    @SuppressFBWarnings(value = "DM_EXIT", justification = "Uncaught exception is critical")
    public void uncaughtException(Thread thread, Throwable exception) {
      synchronized (PTP.this) {
        logger.log(Level.SEVERE, "Thread " + thread.getName()
            + " threw Exception: " + exception.getMessage());
        logger.log(Level.SEVERE, "Shutting down");
      }
      exception.printStackTrace();
      System.exit(1);
    }
  }

  private void addShutdownHook() {
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        if (clientThread != null && !initialized) {
          clientThread.interrupt();
          try {
            clientThread.join();
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }

        exit();
      }
    });
  }
}
