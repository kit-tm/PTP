package edu.kit.tm.ptp.connection;

import edu.kit.tm.ptp.Identifier;
import edu.kit.tm.ptp.ReceiveListener;
import edu.kit.tm.ptp.SendListener;
import edu.kit.tm.ptp.SendListener.State;
import edu.kit.tm.ptp.TorManager;
import edu.kit.tm.ptp.auth.AuthenticationListener;
import edu.kit.tm.ptp.auth.AuthenticatorFactory;
import edu.kit.tm.ptp.auth.PublicKeyAuthenticatorFactory;
import edu.kit.tm.ptp.channels.ChannelListener;
import edu.kit.tm.ptp.channels.ChannelManager;
import edu.kit.tm.ptp.channels.MessageChannel;
import edu.kit.tm.ptp.crypt.CryptHelper;
import edu.kit.tm.ptp.thread.Waker;
import edu.kit.tm.ptp.utility.Constants;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages sending and receiving messages from several hidden services.
 *
 * @author Timon Hackenjos
 */
public class ConnectionManager implements Runnable, ChannelListener, AuthenticationListener, TorManager.SOCKSProxyListener {
  protected String socksHost = null;
  protected volatile int socksPort = -1;
  protected int hsPort;
  private Thread thread = new Thread(this);
  protected SendListener sendListener = null;
  protected ReceiveListener receiveListener = null;
  protected Identifier localIdentifier = null;
  protected final Logger logger = Logger.getLogger(ConnectionManager.class.getName());
  protected ChannelManager channelManager = new ChannelManager(this);

  protected Map<Identifier, MessageChannel> identifierMap = new HashMap<>();
  protected Map<MessageChannel, Identifier> channelMap = new HashMap<>();

  protected Map<MessageChannel, Context> channelContexts = new HashMap<>();

  protected Map<Identifier, Long> lastTry = new HashMap<>();

  /** Messages which have already been dispatched to a channel. */
  protected Map<Long, MessageAttempt> dispatchedMessages = new HashMap<>();

  private AtomicLong messageId = new AtomicLong(0);

  /** Messages which have to be dispatched to an appropriate channel. */
  protected Queue<MessageAttempt> messageQueue = new ConcurrentLinkedQueue<>();

  private Queue<SentMessage> sentMessages = new ConcurrentLinkedQueue<>();
  private Queue<MessageChannel> newConnections = new ConcurrentLinkedQueue<>();
  protected Queue<MessageChannel> closedConnections = new ConcurrentLinkedQueue<>();
  private Queue<ReceivedMessage> receivedMessages = new ConcurrentLinkedQueue<>();
  private Queue<ChannelIdentifier> authQueue = new ConcurrentLinkedQueue<>();
  private Semaphore semaphore = new Semaphore(0);
  private Waker waker = new Waker(semaphore);
  protected AuthenticatorFactory authFactory = new PublicKeyAuthenticatorFactory();
  protected CryptHelper cryptHelper;

  private static class ReceivedMessage {
    public byte[] data;
    public MessageChannel source;

    public ReceivedMessage(byte[] data, MessageChannel source) {
      this.data = data;
      this.source = source;
    }
  }

  private static class SentMessage {
    public long id;
    public MessageChannel destination;

    public SentMessage(long id, MessageChannel destination) {
      this.id = id;
      this.destination = destination;
    }
  }

  private static class ChannelIdentifier {
    public MessageChannel channel;
    public Identifier identifier;

    public ChannelIdentifier(MessageChannel channel, Identifier identifier) {
      this.channel = channel;
      this.identifier = identifier;
    }
  }

  /**
   * Construct a new ConnectionManager.
   *
   * @param hsPort The port to reach PTP hidden services from remote.
   */
  public ConnectionManager(CryptHelper cryptHelper, int hsPort) {
    this.hsPort = hsPort;
    this.cryptHelper = cryptHelper;
  }
  
  @Override
  public void updateSOCKSProxy(String socksHost, int socksProxyPort) {
    if (socksProxyPort == 0 || socksProxyPort < -1 || socksHost == null) {
      throw new IllegalArgumentException();
    }

    this.socksPort = socksProxyPort;
    this.socksHost = socksHost;
  }

  public void setSendListener(SendListener listener) {
    this.sendListener = listener;
  }

  public void setReceiveListener(ReceiveListener listener) {
    this.receiveListener = listener;
  }

  /**
   * Sets the used authentication method by supplying a factory to create authenticator objects.
   */
  public void setAuthenticatorFactory(AuthenticatorFactory factory) {
    this.authFactory = factory;
  }

  /**
   * Starts an own thread for the ConnectionManager.
   */
  public void start() throws IOException {
    logger.log(Level.INFO, "Starting ConnectionManager");
    thread.start();
    channelManager.start();
    waker.start();
    logger.log(Level.INFO, "ConnectionManager started");
  }

  /**
   * Stops the thread. Does nothing if the thread has been stopped before.
   */
  public void stop() {
    logger.log(Level.INFO, "Stopping ConnectionManager");

    thread.interrupt();
    semaphore.release();

    logger.log(Level.INFO, "Waiting for Thread to stop");

    try {
      // Does nothing if thread isn't running
      thread.join();
    } catch (InterruptedException e) {
      logger.log(Level.WARNING, "Failed to wait for thread to stop: " + e.getMessage());
    }

    logger.log(Level.INFO, "Stopping channel manager");

    channelManager.stop();

    logger.log(Level.INFO, "Stopping waker");

    waker.stop();

    logger.log(Level.INFO, "ConnectionManager stopped");
  }

  /**
   * Runs a new bind server on the loopback interface.
   * 
   * @return The port the server listens on.
   * @throws IOException If the binding fails.
   */
  public int startBindServer(int localPort) throws IOException {
    logger.log(Level.INFO, "Starting bind server");
    ServerSocketChannel server = ServerSocketChannel.open();
    server.socket().bind(new InetSocketAddress(Constants.localhost, localPort));
    server.configureBlocking(false);

    channelManager.addServerSocket(server);

    logger.log(Level.INFO, "Started bind server on port " + server.socket().getLocalPort());

    return server.socket().getLocalPort();
  }

  /**
   * Sends a message to the specified destination.
   * 
   * @param data The bytes to send.
   * @param destination The destination to send to.
   * @param timeout How long to wait for a successful sending.
   * @return Identifier for the message.
   */
  public long send(byte[] data, Identifier destination, long timeout) {
    return send(data, destination, timeout, true);
  }

  public long send(byte[] data, Identifier destination, long timeout, boolean informSendListener) {
    long id = messageId.getAndIncrement();
    MessageAttempt attempt = new MessageAttempt(id, System.currentTimeMillis(), data, timeout,
        destination, informSendListener);
    messageQueue.add(attempt);

    logger.log(Level.INFO, "Assigned id " + id + " to message attempt for identifier " + destination
        + " with size " + data.length + " bytes");

    semaphore.release();

    return id;
  }


  /**
   * Closes an open connection to the supplied identifier.
   */
  public void disconnect(Identifier destination) {
    logger.log(Level.INFO, "Disconnecting channel to identifier " + destination);
    MessageChannel channel = identifierMap.get(destination);
    if (channel == null) {
      logger.log(Level.WARNING, "Called disconnect for identifier without connected channel");
      return;
    }

    closedConnections.add(channel);
    semaphore.release();
  }

  @Override
  public void messageSent(long id, MessageChannel destination) {
    logger.log(Level.INFO, "Message with id " + id + " sent successfully");
    sentMessages.add(new SentMessage(id, destination));
    semaphore.release();
  }

  @Override
  public void messageReceived(byte[] data, MessageChannel source) {
    // data doesn't need to be copied because MessageChannels
    // use a new buffer for each message
    receivedMessages.add(new ReceivedMessage(data, source));
    semaphore.release();
  }

  @Override
  public void channelOpened(MessageChannel channel) {
    newConnections.add(channel);
    semaphore.release();
  }

  @Override
  public void channelClosed(MessageChannel channel) {
    closedConnections.add(channel);
    semaphore.release();
  }

  @Override
  public void authenticationSuccess(MessageChannel channel, Identifier identifier) {
    authQueue.add(new ChannelIdentifier(channel, identifier));
    semaphore.release();
  }

  @Override
  public void authenticationFailed(MessageChannel channel) {
    authQueue.add(new ChannelIdentifier(channel, null));
    semaphore.release();
  }

  public void setLocalIdentifier(Identifier localIdentifier) {
    if (localIdentifier == null || !localIdentifier.isValid()) {
      throw new IllegalArgumentException("Identifier is invalid.");
    }

    this.localIdentifier = localIdentifier;
    logger.log(Level.INFO, "Set local identifier to " + localIdentifier);
  }

  private void processNewConnections() {
    MessageChannel channel;
    Context context;

    while ((channel = newConnections.poll()) != null) {
      context = channelContexts.get(channel);

      // check if a context object already exists
      if (context == null) {
        // if it's an incoming connection there is no context object
        // create one
        context = new Context(this);
        channelContexts.put(channel, context);
      }

      context.opened(channel);
    }
  }

  protected MessageChannel connect(Identifier destination) throws IOException {
    logger.log(Level.INFO, "Trying to connect to identifer " + destination);

    SocketChannel socket = SocketChannel.open();
    socket.configureBlocking(false);
    socket.connect(new InetSocketAddress(socksHost, socksPort));

    MessageChannel channel = channelManager.connect(socket);
    return channel;
  }

  private void processMessageAttempts() {
    Identifier identifier;
    MessageChannel channel;
    MessageAttempt attempt;
    Queue<MessageAttempt> tmpQueue = new LinkedList<MessageAttempt>();
    Context context;

    while ((attempt = messageQueue.poll()) != null) {
      identifier = attempt.getDestination();

      // Check if identifier is valid
      if (!identifier.isValid()) {
        sendListener.messageSent(attempt.getId(), identifier, State.INVALID_DESTINATION);
        continue;
      }

      // Check timeout of message
      if (attempt.getTimeout() != -1
          && System.currentTimeMillis() - attempt.getSendTimestamp() >= attempt.getTimeout()) {
        if (attempt.isInformSendListener()) {
          sendListener.messageSent(attempt.getId(), attempt.getDestination(), State.TIMEOUT);
        }
        continue;
      }

      channel = identifierMap.get(identifier);
      context = channelContexts.get(channel);

      if (context == null) {
        // No channel exists for the destination yet
        context = new Context(this);
      }

      if (!context.sendMessage(attempt)) {
        tmpQueue.add(attempt);
      }
    }

    if (tmpQueue.size() > 0) {
      logger.log(Level.INFO, tmpQueue.size() + " unsent message(s) in queue");
      // Wake thread after some time
      waker.wake(5 * 1000);
    }

    messageQueue.addAll(tmpQueue);
  }

  private void processClosedConnections() {
    MessageChannel channel;
    Context context;

    while ((channel = closedConnections.poll()) != null) {
      context = channelContexts.get(channel);

      if (context == null) {
        logger.log(Level.WARNING, "Closing unregistered channel");
        continue;
      }

      context.close(channel);
    }
  }

  private void processSentMessages() {
    SentMessage message;
    Context context;

    while ((message = sentMessages.poll()) != null) {
      context = channelContexts.get(message.destination);

      if (context == null) {
        logger.log(Level.WARNING, "Message sent by channel without a context.");
        continue;
      }

      context.messageSent(message.id, message.destination);
    }
  }

  private void processReceivedMessages() {
    ReceivedMessage message;
    Context context;

    while ((message = receivedMessages.poll()) != null) {
      context = channelContexts.get(message.source);

      if (context == null) {
        logger.log(Level.WARNING, "Message received by a channel without a context.");
        continue;
      }

      context.messageReceived(message.data, message.source);
    }
  }

  private void processAuthAttempts() {
    ChannelIdentifier channelIdentifier;
    MessageChannel channel;
    Identifier identifier;
    Context context;

    while ((channelIdentifier = authQueue.poll()) != null) {
      channel = channelIdentifier.channel;
      identifier = channelIdentifier.identifier;
      context = channelContexts.get(channel);

      context.authenticated(channel, identifier);
    }
  }

  @Override
  public void run() {
    logger.log(Level.INFO, "ConnectionManager thread is running");

    while (!thread.isInterrupted()) {

      try {
        semaphore.acquire();
        semaphore.drainPermits();

        /*
         * The order of the following method calls matter. processSentMessages() and
         * processReceivedMessages() should be called before processClosedConnections()
         */

        // call sendListeners
        processSentMessages();

        // call receiveListeners
        processReceivedMessages();

        // dispatch messages to channels
        processMessageAttempts();

        // Check new connections
        processNewConnections();

        // authentication
        processAuthAttempts();

        // Check closed connections
        processClosedConnections();

      } catch (InterruptedException ie) {
        thread.interrupt();
      }
    }

    logger.log(Level.INFO, "ConnectionManager thread finishes execution");
  }

  public CryptHelper getCryptHelper() {
    return cryptHelper;
  }
}
