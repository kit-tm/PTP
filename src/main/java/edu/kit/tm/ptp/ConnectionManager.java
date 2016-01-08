package edu.kit.tm.ptp;

import edu.kit.tm.ptp.SendListener.State;
import edu.kit.tm.ptp.channels.ChannelListener;
import edu.kit.tm.ptp.channels.ChannelManager;
import edu.kit.tm.ptp.channels.MessageChannel;
import edu.kit.tm.ptp.channels.SOCKSChannel;
import edu.kit.tm.ptp.serialization.Serializer;
import edu.kit.tm.ptp.utility.Constants;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
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
 * Class.
 *
 * @author Timon Hackenjos
 */
public class ConnectionManager implements Runnable, ChannelListener, AuthenticationListener {
  public enum ConnectionState {
    CLOSED, CONNECT, CONNECT_SOCKS, CONNECTED, AUTHENTICATED
  }

  private String socksHost;
  private int socksPort;
  private int hsPort;
  private Thread thread;
  private SendListener sendListener = null;
  private ReceiveListener receiveListener = null;
  private Identifier localIdentifier = null;

  private ChannelManager channelManager = new ChannelManager(this);
  private Map<Identifier, MessageChannel> identifierMap = new HashMap<>();
  private Map<MessageChannel, Identifier> channelMap = new HashMap<>();
  private List<MessageAttempt> sentMessages = new LinkedList<>();
  private Map<MessageChannel, ConnectionState> connectionStates = new HashMap<>();
  private Map<Identifier, Long> lastTry = new HashMap<>();

  // TODO Threadsicherheit überprüfen
  private AtomicLong messageId = new AtomicLong(0);
  private Queue<MessageAttempt> waitingQueue = new ConcurrentLinkedQueue<>();
  private Queue<Long> sentMessageIds = new ConcurrentLinkedQueue<>();
  private Queue<MessageChannel> newConnections = new ConcurrentLinkedQueue<>();
  private Queue<MessageChannel> closedConnections = new ConcurrentLinkedQueue<>();
  private Queue<ReceivedMessage> receivedMessages = new ConcurrentLinkedQueue<>();
  private Queue<ChannelIdentifier> authQueue = new ConcurrentLinkedQueue<>();
  private Semaphore semaphore = new Semaphore(0);
  private Waker waker = new Waker(semaphore);

  private Serializer serializer;
  private final Logger logger = Logger.getLogger(Constants.connectionManagerLogger);
  private final long connectIntervall = 30 * 1000;

  private class ReceivedMessage {
    public byte[] data;
    public MessageChannel source;

    public ReceivedMessage(byte[] data, MessageChannel source) {
      this.data = data;
      this.source = source;
    }
  }

  private class ChannelIdentifier {
    public MessageChannel channel;
    public Identifier identifier;

    public ChannelIdentifier(MessageChannel channel, Identifier identifier) {
      this.channel = channel;
      this.identifier = identifier;
    }
  }

  public ConnectionManager(String socksHost, int socksPort, int hsPort) {
    this.socksHost = socksHost;
    this.socksPort = socksPort;
    this.hsPort = hsPort;
  }

  public void setSerializer(Serializer serializer) {
    this.serializer = serializer;
  }

  public void setSendListener(SendListener listener) {
    this.sendListener = listener;
  }

  public void setReceiveListener(ReceiveListener listener) {
    this.receiveListener = listener;
  }

  public void start() throws IOException {
    logger.log(Level.INFO, "Starting ConnectionManager");
    thread = new Thread(this);
    thread.start();
    channelManager.start();
    waker.start();
    logger.log(Level.INFO, "ConnectionManager started");
  }

  public void stop() throws IOException {
    logger.log(Level.INFO, "Stopping ConnectionManager");
    thread.interrupt();
    semaphore.release();

    logger.log(Level.INFO, "Waiting for Thread to stop");

    while (thread.isAlive()) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        // Sleeping was interrupted
      }
    }

    logger.log(Level.INFO, "Stopping channel manager");

    channelManager.stop();

    logger.log(Level.INFO, "Stopping waker");

    waker.stop();

    logger.log(Level.INFO, "ConnectionManager stopped");
  }

  public int startBindServer() throws IOException {
    logger.log(Level.INFO, "Starting bind server");
    ServerSocketChannel server = ServerSocketChannel.open();
    server.socket()
        .bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), Constants.anyport));
    server.configureBlocking(false);

    channelManager.addServerSocket(server);

    logger.log(Level.INFO, "Started bind server on port " + server.socket().getLocalPort());

    return server.socket().getLocalPort();
  }

  long send(byte[] data, Identifier destination, long timeout) {
    long id = messageId.getAndIncrement();
    MessageAttempt attempt =
        new MessageAttempt(id, System.currentTimeMillis(), data, timeout, destination);
    if (!waitingQueue.offer(attempt)) {
      logger.log(Level.WARNING, "Can't add message attempt to queue");
      return id;
    }

    logger.log(Level.INFO, "Assigned id " + id + " to message attempt for identifier " + destination
        + " with size " + data.length + " bytes");

    semaphore.release();

    return id;
  }

  private MessageChannel connect(Identifier destination) throws IOException {
    logger.log(Level.INFO, "Trying to connect to identifer " + destination);

    SocketChannel socket = SocketChannel.open();
    socket.configureBlocking(false);
    socket.connect(new InetSocketAddress(socksHost, socksPort));

    MessageChannel channel = channelManager.connect(socket);
    return channel;
  }

  public void disconnect(Identifier destination) {
    logger.log(Level.INFO, "Disconnecting channel to identifier " + destination);
    MessageChannel channel = identifierMap.get(destination);
    if (channel == null) {
      logger.log(Level.WARNING, "Called disconnect for identifier without connected channel");
      return;
    }

    if (!closedConnections.offer(channel)) {
      logger.log(Level.WARNING, "Can't add channel to close to queue");
      return;
    }
    semaphore.release();
  }

  @Override
  public void messageSent(long id, MessageChannel destination) {
    logger.log(Level.INFO, "Message with id " + id + " sent successfully");
    if (!sentMessageIds.offer(id)) {
      logger.log(Level.WARNING, "Can't add message to queue");
      return;
    }
    semaphore.release();
  }

  @Override
  public void messageReceived(byte[] data, MessageChannel source) {
    if (!receivedMessages.offer(new ReceivedMessage(data, source))) {
      logger.log(Level.WARNING, "Failed to add received message to queue");
      return;
    }
    semaphore.release();
  }

  @Override
  public void channelOpened(MessageChannel channel) {
    if (!newConnections.offer(channel)) {
      logger.log(Level.WARNING, "Failed to add new channel to queue");
      return;
    }
    semaphore.release();
  }

  @Override
  public void channelClosed(MessageChannel channel) {
    if (!closedConnections.offer(channel)) {
      logger.log(Level.WARNING, "Failed to add channel to remove to queue");
      return;
    }
    semaphore.release();
  }

  private void processNewConnections() {
    ConnectionState state;
    Identifier identifier;
    MessageChannel channel;

    while ((channel = newConnections.poll()) != null) {
      identifier = channelMap.get(channel);

      if (identifier == null) {
        // Incoming connection
        try {
          logger.log(Level.INFO,
              "Received new connection from " + channel.getChannel().getRemoteAddress().toString());
        } catch (IOException ioe) {
          logger.log(Level.WARNING, "Failed to get remote address of new channel", ioe);
          continue;
        }

        try {
          channelManager.addChannel(channel);
          connectionStates.put(channel, ConnectionState.CONNECTED);

          Authenticator auth = new DummyAuthenticator(this, channel, serializer);
          auth.authenticate(localIdentifier);
        } catch (ClosedChannelException e) {
          logger.log(Level.WARNING, "Channel was closed while adding channel to ChannelManager", e);
          continue;
        }
      } else {
        state = connectionStates.get(channel);

        // TODO what to do with an incoming connection from a hs for which a connection is already
        // established
        switch (state) {
          case CONNECT:
            logger.log(Level.INFO,
                "Trying to connect to " + identifier + " through tor socks proxy");
            // channel isn't registered at channel manager
            identifierMap.remove(identifier);
            channelMap.remove(channel);
            connectionStates.remove(channel);

            SOCKSChannel socks = new SOCKSChannel(channel, channelManager);
            socks.connetThroughSOCKS(identifier.getTorAddress(), hsPort);
            try {
              channelManager.addChannel(socks);

              identifierMap.put(identifier, socks);
              channelMap.put(socks, identifier);
              connectionStates.put(socks, ConnectionState.CONNECT_SOCKS);
            } catch (ClosedChannelException e) {
              logger.log(Level.WARNING, "Channel was closed while adding channel to ChannelManager",
                  e);
              identifierMap.remove(identifier);
              channelMap.remove(socks);
              connectionStates.remove(socks);
            }
            break;
          case CONNECT_SOCKS:
            logger.log(Level.INFO,
                "Connection to " + identifier + " through socks was successfull");
            connectionStates.put(channel, ConnectionState.CONNECTED);

            if (localIdentifier == null) {
              logger.log(Level.WARNING, "No identifier set. Unable to authenticate the connection");
              channelManager.removeChannel(channel);
              identifierMap.remove(identifier);
              channelMap.remove(channel);
              connectionStates.remove(channel);
              continue;
            }

            logger.log(Level.INFO, "Trying to authenticate connection to " + identifier);
            Authenticator auth = new DummyAuthenticator(this, channel, serializer);
            auth.authenticate(localIdentifier);
            break;
          default:
            throw new IllegalStateException();
        }
      }
    }
  }

  private void processMessageAttempts() {
    // TODO Thread kann nicht einschlafen, falls eine unzustellbare Nachricht in der Warteschlange
    // ist
    ConnectionState state;
    Identifier identifier;
    MessageChannel channel;
    MessageAttempt attempt;
    Queue<MessageAttempt> tmpQueue = new LinkedList<MessageAttempt>();

    while ((attempt = waitingQueue.poll()) != null) {
      identifier = attempt.getDestination();
      if (!identifier.isValid()) {
        sendListener.messageSent(attempt.getId(), identifier, State.CONNECTION_TIMEOUT);
        continue;
      }

      if (!attempt.isRegistered()) {
        sentMessages.add(attempt);
        attempt.setRegistered(true);
      }

      channel = identifierMap.get(identifier);
      state = channel != null ? connectionStates.get(channel) : null;

      if (state == null) {
        state = ConnectionState.CLOSED;
      }

      switch (state) {
        case AUTHENTICATED:
          logger.log(Level.INFO,
              "Sending message with id " + attempt.getId() + " to " + attempt.getDestination());

          if (channel == null) {
            throw new IllegalStateException();
          }

          if (channel.isIdle()) {
            channel.addMessage(attempt.getData(), attempt.getId());
          } else {
            if (!tmpQueue.offer(attempt)) {
              logger.log(Level.WARNING, "Failed to add message attempt to queue");
              continue;
            }
          }
          break;
        case CLOSED:
          logger.log(Level.INFO, "Connection to destination " + identifier + " is closed");
          if (lastTry.get(identifier) == null
              || System.currentTimeMillis() - lastTry.get(identifier) >= connectIntervall) {
            logger.log(Level.INFO, "Opening new connection to destination " + identifier);
            lastTry.put(identifier, System.currentTimeMillis());
            try {
              channel = connect(identifier);

              identifierMap.put(identifier, channel);
              channelMap.put(channel, identifier);
              connectionStates.put(channel, ConnectionState.CONNECT);
            } catch (IOException ioe) {
              logger.log(Level.WARNING,
                  "Error while trying to open a new connection to " + identifier, ioe);
              identifierMap.remove(identifier);
              channelMap.remove(channel);
              connectionStates.remove(channel);
            }
          }
        // continue with default case
        default:
          if (!tmpQueue.offer(attempt)) {
            logger.log(Level.WARNING, "Failed to add message attempt to queue");
            continue;
          }
          break;
      }
    }

    if (tmpQueue.size() > 0) {
      logger.log(Level.INFO, tmpQueue.size() + " unsent message(s) in queue");
      // Wake thread after some time
      waker.wake(5 * 1000);
    }

    for (MessageAttempt message : tmpQueue) {
      if (!waitingQueue.offer(message)) {
        logger.log(Level.WARNING, "Failed to add message attempt to queue");
      }
    }
  }

  private void processClosedConnections() {
    MessageChannel channel;
    Identifier identifier;
    ConnectionState state;

    while ((channel = closedConnections.poll()) != null) {
      state = connectionStates.get(channel);
      identifier = channelMap.get(channel);

      if (state == null) {
        logger.log(Level.WARNING, "Closing unregistered channel");
      }

      channelManager.removeChannel(channel);

      try {
        channel.getChannel().close();
      } catch (IOException e) {
        logger.log(Level.INFO, "Error while trying to close channel");
      }

      channelMap.remove(channel);
      connectionStates.remove(channel);

      if (identifier != null) {
        identifierMap.remove(identifier);
      }

      logger.log(Level.INFO,
          "Closed connection to identifier " + (identifier != null ? identifier.toString() : ""));
    }
  }

  private void processSentMessages() {
    Long id;
    MessageAttempt attempt;

    while ((id = sentMessageIds.poll()) != null) {

      boolean found = false;

      Iterator<MessageAttempt> it = sentMessages.iterator();

      while (it.hasNext() && !found) {
        attempt = it.next();

        if (attempt.getId() == id) {
          it.remove();
          if (sendListener != null) {
            sendListener.messageSent(id, attempt.getDestination(), State.SUCCESS);
          }
          found = true;
        }

      }

      if (!found) {
        logger.log(Level.WARNING, "Unknown message id of sent message " + id);
        throw new IllegalStateException();
      }
    }
  }

  private void processReceivedMessages() {
    ReceivedMessage message;

    while ((message = receivedMessages.poll()) != null) {
      Identifier identifier = channelMap.get(message.source);

      if (identifier == null) {
        logger.log(Level.WARNING,
            "Received message with size " + message.data.length + " from unknown channel");
        continue;
      }

      logger.log(Level.INFO,
          "Received message from " + identifier + " with size " + message.data.length);

      if (receiveListener != null) {
        receiveListener.messageReceived(message.data, identifier);
      }
    }
  }

  private void processAuthAttempts() {
    ChannelIdentifier channelIdentifier;
    MessageChannel channel;
    Identifier identifier;

    while ((channelIdentifier = authQueue.poll()) != null) {
      channel = channelIdentifier.channel;
      identifier = channelIdentifier.identifier;

      if (identifier == null) {
        // Auth wasn't successfull
        identifier = channelMap.get(channel);

        if (identifier == null) {
          logger.log(Level.WARNING, "Authentication attempt on non-registered channel");
        } else {
          logger.log(Level.INFO, "Authenticating connection to " + identifier + " failed");
        }
        if (!closedConnections.offer(channel)) {
          logger.log(Level.WARNING, "Failed to add connection to close to queue");
        }
      } else {
        // Auth was successfull
        connectionStates.put(channel, ConnectionState.AUTHENTICATED);
        // TODO check for other connections to identifier
        identifierMap.put(identifier, channel);
        channelMap.put(channel, identifier);
        logger.log(Level.INFO,
            "Connection to " + identifier + " has been authenticated successfully");
      }
    }
  }

  @Override
  public void run() {
    while (!thread.isInterrupted()) {

      try {
        // TODO Problematisch?
        semaphore.acquire();
        semaphore.drainPermits();

        // deliver messages
        processMessageAttempts();

        // Check new connections
        processNewConnections();

        // call sendListeners
        processSentMessages();

        // authentication
        processAuthAttempts();

        // call receiveListeners
        processReceivedMessages();

        // Check closed connections
        processClosedConnections();

        // Check timeouts for messages

      } catch (InterruptedException ie) {
        thread.interrupt();
      }
    }
  }

  @Override
  public void authenticationSuccess(MessageChannel channel, Identifier identifier) {
    if (!authQueue.offer(new ChannelIdentifier(channel, identifier))) {
      logger.log(Level.WARNING, "Failed to add successfull auth attempt to queue");
    }
    semaphore.release();
  }

  @Override
  public void authenticationFailed(MessageChannel channel) {
    if (!authQueue.offer(new ChannelIdentifier(channel, null))) {
      logger.log(Level.WARNING, "Failed to add failed auth attempt to queue");
    }
    semaphore.release();
  }

  public void setLocalIdentifier(Identifier localIdentifier) {
    this.localIdentifier = localIdentifier;
    logger.log(Level.INFO, "Set local identifier to " + localIdentifier);
  }
}
