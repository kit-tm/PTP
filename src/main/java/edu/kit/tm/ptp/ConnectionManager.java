package edu.kit.tm.ptp;

import edu.kit.tm.ptp.SendListener.State;
import edu.kit.tm.ptp.channels.ChannelListener;
import edu.kit.tm.ptp.channels.ChannelManager;
import edu.kit.tm.ptp.channels.MessageChannel;
import edu.kit.tm.ptp.channels.SOCKSChannel;
import edu.kit.tm.ptp.utility.Constants;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Class.
 *
 * @author Timon Hackenjos
 */
public class ConnectionManager implements Runnable, ChannelListener {
  public enum ConnectionState {
    CLOSED, CONNECT, CONNECT_SOCKS, CONNECTED
  }

  private String socksHost;
  private int socksPort;
  private int hsPort;
  private Thread thread;
  private SendListener sendListener;
  private ReceiveListener receiveListener;

  private ChannelManager channelManager = new ChannelManager(this);
  private Map<Identifier, MessageChannel> identifierMap = new HashMap<>();
  private Map<MessageChannel, Identifier> channelMap = new HashMap<>();
  private Map<Identifier, LinkedList<MessageAttempt>> messageAttempts =
      new HashMap<Identifier, LinkedList<MessageAttempt>>();
  private Map<Identifier, ConnectionState> connectionStates = new HashMap<>();

  // TODO Threadsicherheit überprüfen
  private AtomicLong messageId = new AtomicLong(0);
  private Queue<MessageAttempt> waitingQueue = new ConcurrentLinkedQueue<>();
  private Queue<Long> sentMessages = new ConcurrentLinkedQueue<>();
  private Queue<MessageChannel> newConnections = new ConcurrentLinkedQueue<>();
  private Queue<MessageChannel> closedConnections = new ConcurrentLinkedQueue<>();

  public ConnectionManager(String socksHost, int socksPort, int hsPort) {
    this.socksHost = socksHost;
    this.socksPort = socksPort;
    this.hsPort = hsPort;
  }

  public void start() throws IOException {
    thread = new Thread(this);
    thread.start();
    channelManager.start();
  }

  public void stop() {
    thread.interrupt();
    channelManager.stop();
  }

  long send(byte[] data, Identifier destination, long timeout) {
    long id = messageId.getAndIncrement();
    MessageAttempt attempt =
        new MessageAttempt(id, System.currentTimeMillis(), data, timeout, destination);
    if (!waitingQueue.offer(attempt)) {
      // log error
    }

    return id;
  }

  public void setSendListener(SendListener listener) {
    this.sendListener = listener;
  }

  public void setReceiveListener(ReceiveListener listener) {
    this.receiveListener = listener;
  }

  private MessageChannel connect(Identifier destination) throws IOException {
    SocketChannel socket = SocketChannel.open();
    socket.configureBlocking(false);

    MessageChannel channel = channelManager.connect(socket);
    socket.connect(InetSocketAddress.createUnresolved(socksHost, socksPort));
    return channel;
  }

  public void disconnect(Identifier destination) {

  }

  public int startBindServer() throws IOException {
    ServerSocketChannel server = ServerSocketChannel.open();
    server.socket().bind(new InetSocketAddress(Constants.anyport));
    server.configureBlocking(false);

    channelManager.addServerSocket(server);

    return server.socket().getLocalPort();
  }

  @Override
  public void messageSent(long id, MessageChannel destination) {
    if (!sentMessages.offer(id)) {
      // TODO handle error
    }
  }

  @Override
  public void messageReceived(byte[] data, MessageChannel source) {
    Identifier identifier = channelMap.get(source);

    if (identifier == null) {
      // TODO handle error
      throw new IllegalStateException();
    }

    // Make call with different Thread?
    receiveListener.messageReceived(data, identifier);
  }

  @Override
  public void channelOpened(MessageChannel channel) {
    if (!newConnections.offer(channel)) {
      // TODO log error
    }
  }

  @Override
  public void channelClosed(MessageChannel channel) {
    if (!closedConnections.offer(channel)) {
      // TODO log error
    }
  }

  private void processNewConnections() {
    ConnectionState state;
    Identifier identifier;
    MessageChannel channel;

    while ((channel = newConnections.poll()) != null) {
      identifier = channelMap.get(channel);

      if (identifier == null) {
        // Incoming connection
        // TODO Auth

        // Not yet authenticated
        if (!newConnections.offer(channel)) {
          // TODO log error
        }
      } else {
        state = connectionStates.get(identifier);

        // TODO what to do with an incoming connection from a hs for which a connection is already
        // established
        switch (state) {
          case CONNECT:
            SOCKSChannel socks = new SOCKSChannel(channel);
            socks.connetThroughSOCKS(identifier.getTorAddress(), hsPort);
            identifierMap.put(identifier, socks);
            channelMap.put(socks, identifier);
            channelManager.addChannel(socks);
            connectionStates.put(identifier, ConnectionState.CONNECT_SOCKS);
            break;
          case CONNECT_SOCKS:
            connectionStates.put(identifier, ConnectionState.CONNECTED);
            break;
          default:
            throw new IllegalStateException();
        }
      }
    }
  }

  private void processMessageAttempts() {
    ConnectionState state;
    Identifier identifier;
    MessageChannel channel;
    MessageAttempt attempt;

    while ((attempt = waitingQueue.poll()) != null) {
      identifier = attempt.getDestination();
      state = connectionStates.get(identifier);

      if (state == null) {
        state = ConnectionState.CLOSED;
        connectionStates.put(identifier, state);
      }

      switch (state) {
        case CONNECTED:
          identifierMap.get(identifier).addMessage(attempt.getData(), attempt.getId());
          break;
        case CLOSED:
          try {
            channel = connect(identifier);
            identifierMap.put(identifier, channel);
            channelMap.put(channel, identifier);
          } catch (IOException ioe) {
            // TODO log error
          }
          // continue with default case
        default:
          // TODO check if offer returns true
          waitingQueue.offer(attempt);
          break;
      }
    }
  }

  private void processClosedConnections() {
    MessageChannel channel;
    Identifier identifier;

    while ((channel = closedConnections.poll()) != null) {
      identifier = channelMap.get(channel);
      connectionStates.put(identifier, ConnectionState.CLOSED);

      identifierMap.remove(identifier);
      channelMap.remove(channel);
    }
  }

  private void processSentMessages() {
    Long id;
    MessageAttempt attempt;

    while ((id = sentMessages.poll()) != null) {
      Collection<LinkedList<MessageAttempt>> attemptsCollection = messageAttempts.values();

      boolean found = false;

      for (LinkedList<MessageAttempt> attempts : attemptsCollection) {
        Iterator<MessageAttempt> it = attempts.iterator();

        while (it.hasNext() && !found) {
          attempt = it.next();

          if (attempt.getId() == id) {
            it.remove();
            sendListener.messageSent(id, attempt.getDestination(), State.SUCCESS);
            found = true;
          }
        }
      }

      if (!found) {
        // TODO log error
      }
    }
  }

  @Override
  public void run() {
    // TODO Auto-generated method stub


    while (!Thread.interrupted()) {

      // Check new connections
      processNewConnections();

      // Check closed connections
      processClosedConnections();

      // deliver messages
      processMessageAttempts();

      // call sendListeners
      processSentMessages();

      // Check timeouts for messages
    }
  }
}
