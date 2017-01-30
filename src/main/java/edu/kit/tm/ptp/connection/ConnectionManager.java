package edu.kit.tm.ptp.connection;

import edu.kit.tm.ptp.Configuration;
import edu.kit.tm.ptp.Identifier;
import edu.kit.tm.ptp.ReceiveListener;
import edu.kit.tm.ptp.SendListener;
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

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.HashMap;
import java.util.Iterator;
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
public class ConnectionManager implements Runnable, ChannelListener, AuthenticationListener,
    TorManager.SOCKSProxyListener {
  private final Thread thread;
  private final AtomicLong messageId = new AtomicLong(0);
  private final int sendMessageRetryInterval;

  protected final Semaphore semaphore = new Semaphore(0);
  private final Waker waker;

  protected final int hsPort;
  protected final SendListener sendListener;
  protected final ReceiveListener receiveListener;
  protected final Logger logger = Logger.getLogger(ConnectionManager.class.getName());
  protected final int connectRetryInterval;

  protected final ChannelManager channelManager;
  protected final AuthenticatorFactory authFactory;
  protected final CryptHelper cryptHelper = new CryptHelper();

  protected final Map<Identifier, MessageChannel> identifierMap = new HashMap<>();
  protected final Map<MessageChannel, Identifier> channelMap = new HashMap<>();
  protected final Map<MessageChannel, Context> channelContexts = new HashMap<>();
  protected final Map<Identifier, Long> lastTry = new HashMap<>();
  /** Messages which have already been dispatched to a channel. */
  protected final Map<Long, MessageAttempt> dispatchedMessages = new HashMap<>();
  protected final Queue<Event> eventQueue = new ConcurrentLinkedQueue<>();

  protected String socksHost = null;
  protected int socksPort = -1;
  protected Identifier localIdentifier = null;

  /**
   * Construct a new ConnectionManager.
   *
   * @param hsPort The port to reach PTP hidden services from remote.
   * @param receiveListener The listener to inform about received messages.
   * @param sendListener The listener to inform about sent messages.
   */
  public ConnectionManager(int hsPort, ReceiveListener receiveListener, SendListener sendListener,
                           Configuration config) {
    this (hsPort, receiveListener, sendListener, config, null, new PublicKeyAuthenticatorFactory());
  }

  /**
   * Construct a new ConnectionManager.
   *
   * @param hsPort The port to reach PTP hidden services from remote.
   * @param receiveListener The listener to inform about received messages.
   * @param sendListener The listener to inform about sent messages.
   * @param group The ThreadGroup to start threads in or null.
   */
  public ConnectionManager(int hsPort, ReceiveListener receiveListener, SendListener sendListener,
                           Configuration config, ThreadGroup group) {
    this (hsPort, receiveListener, sendListener, config, group, new PublicKeyAuthenticatorFactory());
  }

  /**
   * Construct a new ConnectionManager.
   *
   * @param hsPort The port to reach PTP hidden services from remote.
   * @param receiveListener The listener to inform about received messages.
   * @param sendListener The listener to inform about sent messages.
   */
  public ConnectionManager(int hsPort, ReceiveListener receiveListener, SendListener sendListener,
                           Configuration config, AuthenticatorFactory authFactory) {
    this (hsPort, receiveListener, sendListener, config, null, authFactory);
  }

  /**
   * Allows to set the used authentication method by supplying a factory
   * to create authenticator objects.
   */
  public ConnectionManager(int hsPort, ReceiveListener receiveListener, SendListener sendListener,
                           Configuration config, ThreadGroup group, AuthenticatorFactory authFactory) {
    if (receiveListener == null || sendListener == null || authFactory == null) {
      throw new IllegalArgumentException();
    }

    this.hsPort = hsPort;
    this.receiveListener = receiveListener;
    this.sendListener = sendListener;
    this.authFactory = authFactory;
    this.channelManager = new ChannelManager(this, group);
    this.waker = new Waker(semaphore, group);
    this.thread = new Thread(group, this);

    if (config == null) {
      this.connectRetryInterval = Configuration.DEFAULT_CONNECTRETRYINTERVAL;
      this.sendMessageRetryInterval = Configuration.DEFAULT_MESSAGESENDRETRYINTERVAL;
    } else {
      this.connectRetryInterval = config.getConnectRetryInterval();
      this.sendMessageRetryInterval = config.getMessageSendRetryInterval();
    }
  }
  
  @Override
  public void updateSOCKSProxy(String socksHost, int socksProxyPort) {
    if (socksProxyPort == 0 || socksProxyPort < -1 || socksHost == null) {
      throw new IllegalArgumentException();
    }

    eventQueue.add(new EventUpdateSOCKS(this, socksHost, socksProxyPort));
    semaphore.release();
  }

  /**
   * Starts an own thread for the ConnectionManager.
   */
  public void start() throws IOException {
    try {
      cryptHelper.init();
    } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
      throw new IOException("Cryptographic algorithm or provider unavailable: " + e.getMessage());
    }

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

    eventQueue.add(new EventSendMessage(this, attempt));

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

    eventQueue.add(new EventConnectionClosed(this, channel));

    semaphore.release();
  }

  /**
   * Sets the identifier of the local hidden service and the private key used for authentication.
   */
  public void setIdentity(File privateKey, Identifier identifier) {
    if (privateKey == null) {
      throw new IllegalArgumentException();
    }

    eventQueue.add(new EventSetIdentity(this, privateKey, identifier));
    semaphore.release();
  }

  /**
   * Sets the identifier of the local hidden service.
   */
  public void setLocalIdentifier(Identifier localIdentifier) {
    if (localIdentifier == null || !localIdentifier.isValid()) {
      throw new IllegalArgumentException("Identifier is invalid.");
    }

    eventQueue.add(new EventSetIdentifier(this, localIdentifier));
    semaphore.release();
  }

  @Override
  public void messageSent(long id, MessageChannel destination) {
    logger.log(Level.INFO, "Message with id " + id + " sent successfully");

    eventQueue.add(new EventMessageSent(this, id, destination));
    semaphore.release();
  }

  @Override
  public void messageReceived(byte[] data, MessageChannel source) {
    // data doesn't need to be copied because MessageChannels
    // use a new buffer for each message
    eventQueue.add(new EventMessageReceived(this, data, source));
    semaphore.release();
  }

  @Override
  public void channelOpened(MessageChannel channel) {
    eventQueue.add(new EventConnectionOpened(this, channel));
    semaphore.release();
  }

  @Override
  public void channelClosed(MessageChannel channel) {
    eventQueue.add(new EventConnectionClosed(this, channel));
    semaphore.release();
  }

  @Override
  public void authenticationSuccess(MessageChannel channel, Identifier identifier) {
    authenticationFinished(channel, identifier);
  }

  @Override
  public void authenticationFailed(MessageChannel channel) {
    authenticationFinished(channel, null);
  }
  
  private void authenticationFinished(MessageChannel channel, Identifier identifier) {
    Context context = channelContexts.get(channel);
    
    if (context == null) {
      logger.log(Level.WARNING, "Authentication on unregistered channel");
    } else {
      context.authenticated(channel, identifier);
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

  @Override
  public void run() {
    logger.log(Level.INFO, "ConnectionManager thread is running");

    Iterator<Event> it;
    Event event;
    long unprocessed;

    while (!thread.isInterrupted()) {

      try {
        semaphore.acquire();
        semaphore.drainPermits();

        it = eventQueue.iterator();
        unprocessed = 0;

        while (it.hasNext()) {
          event = it.next();

          if (event.process()) {
            it.remove();
          } else {
            unprocessed++;
          }
        }

        // Only EventSendMessage returns false
        // unprocessed = messages in queue
        if (unprocessed > 0) {
          logger.log(Level.INFO, unprocessed + " unsent message(s) in queue");
          // Wake thread after some time
          waker.wake(sendMessageRetryInterval);
        }

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
