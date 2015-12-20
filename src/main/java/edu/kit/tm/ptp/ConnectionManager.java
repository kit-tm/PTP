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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

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
  private Map<Identifier, LinkedList<MessageAttempt>> messageAttempts =
      new HashMap<Identifier, LinkedList<MessageAttempt>>();
  private Map<Identifier, ConnectionState> connectionStates = new HashMap<>();

  // TODO Threadsicherheit überprüfen
  private AtomicLong messageId = new AtomicLong(0);
  private Queue<MessageAttempt> waitingQueue = new ConcurrentLinkedQueue<>();
  private Queue<Long> sentMessages = new ConcurrentLinkedQueue<>();
  private Queue<MessageChannel> newConnections = new ConcurrentLinkedQueue<>();
  private Queue<MessageChannel> closedConnections = new ConcurrentLinkedQueue<>();
  private Queue<ReceivedMessage> receivedMessages = new ConcurrentLinkedQueue<>();
  private Queue<ChannelIdentifier> authQueue = new ConcurrentLinkedQueue<>();
  private Semaphore semaphore = new Semaphore(0);
  
  private Serializer serializer;

  private class ReceivedMessage {
    public byte[] data;
    public Identifier source;

    public ReceivedMessage(byte[] data, Identifier source) {
      this.data = data;
      this.source = source;
    }
  }

  public ConnectionManager(String socksHost, int socksPort, int hsPort) {
    this.socksHost = socksHost;
    this.socksPort = socksPort;
    this.hsPort = hsPort;
    this.serializer = serializer;
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
    thread = new Thread(this);
    thread.start();
    channelManager.start();
  }

  public void stop() throws IOException {
    thread.interrupt();
    channelManager.stop();
  }
  
  public int startBindServer() throws IOException {
    ServerSocketChannel server = ServerSocketChannel.open();
    server.socket()
        .bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), Constants.anyport));
    server.configureBlocking(false);

    channelManager.addServerSocket(server);

    return server.socket().getLocalPort();
  }

  long send(byte[] data, Identifier destination, long timeout) {
    long id = messageId.getAndIncrement();
    MessageAttempt attempt =
        new MessageAttempt(id, System.currentTimeMillis(), data, timeout, destination);
    if (!waitingQueue.offer(attempt)) {
      // log error
    }
    
    semaphore.release();

    return id;
  }

  private MessageChannel connect(Identifier destination) throws IOException {
    SocketChannel socket = SocketChannel.open();
    socket.configureBlocking(false);
    socket.connect(new InetSocketAddress(socksHost, socksPort));
    
    MessageChannel channel = channelManager.connect(socket);
    return channel;
  }

  public void disconnect(Identifier destination) {
    MessageChannel channel = identifierMap.get(destination);
    if (channel == null) {
      // TODO log error
      return;
    }

    closedConnections.offer(channel);
    // TODO check offer return value
    semaphore.release();
  }

  @Override
  public void messageSent(long id, MessageChannel destination) {
    if (!sentMessages.offer(id)) {
      // TODO handle error
    }
    semaphore.release();
  }

  @Override
  public void messageReceived(byte[] data, MessageChannel source) {

    Identifier identifier = channelMap.get(source);

    if (identifier == null) {
      // TODO handle error
      throw new IllegalStateException();
    }

    receivedMessages.offer(new ReceivedMessage(data, identifier));
    semaphore.release();
  }

  @Override
  public void channelOpened(MessageChannel channel) {
    if (!newConnections.offer(channel)) {
      // TODO log error
    }
    semaphore.release();
  }

  @Override
  public void channelClosed(MessageChannel channel) {
    if (!closedConnections.offer(channel)) {
      // TODO log error
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
        // TODO Auth
        try {
          channelManager.addChannel(channel);
          
          Authenticator auth = new DummyAuthenticator(this, channel, serializer);
          // TODO need own identifier
          auth.authenticate(localIdentifier);
        } catch (ClosedChannelException e) {
          // TODO handle exception
        }
      } else {
        state = connectionStates.get(identifier);

        // TODO what to do with an incoming connection from a hs for which a connection is already
        // established
        switch (state) {
          case CONNECT:
            SOCKSChannel socks = new SOCKSChannel(channel, channelManager);
            socks.connetThroughSOCKS(identifier.getTorAddress(), hsPort);
            try {
              channelManager.addChannel(socks);

              identifierMap.put(identifier, socks);
              channelMap.put(socks, identifier);
              connectionStates.put(identifier, ConnectionState.CONNECT_SOCKS);
            } catch (ClosedChannelException e) {
              // TODO log error
            }
            break;
          case CONNECT_SOCKS:
            connectionStates.put(identifier, ConnectionState.CONNECTED);
            break;
          case CONNECTED:
            Authenticator auth = new DummyAuthenticator(this, channel, serializer);
            // TODO need own identifier
            auth.authenticate(localIdentifier);
            break;
          default:
            throw new IllegalStateException();
        }
      }
    }
  }

  private void processMessageAttempts() {
    // TODO Thread kann nicht einschlafen, falls eine unzustellbare Nachricht in der Warteschlange ist
    ConnectionState state;
    Identifier identifier;
    MessageChannel channel;
    MessageAttempt attempt;
    Queue<MessageAttempt> tmpQueue = new LinkedList<MessageAttempt>();
 
    while ((attempt = waitingQueue.poll()) != null) {
      identifier = attempt.getDestination();
      state = connectionStates.get(identifier);

      if (state == null) {
        state = ConnectionState.CLOSED;
      }

      switch (state) {
        case AUTHENTICATED:
          identifierMap.get(identifier).addMessage(attempt.getData(), attempt.getId());
          break;
        case CLOSED:
          try {
            channel = connect(identifier);
            identifierMap.put(identifier, channel);
            channelMap.put(channel, identifier);
            connectionStates.put(identifier, ConnectionState.CONNECT);
          } catch (IOException ioe) {
            // TODO log error
          }
          // continue with default case
        default:
          // TODO check if offer returns true
          //waitingQueue.offer(attempt);
          tmpQueue.offer(attempt);
          semaphore.release();
          break;
      }
    }
    
    for (MessageAttempt message: tmpQueue) {
      waitingQueue.offer(message);
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
            if (sendListener != null) {
              sendListener.messageSent(id, attempt.getDestination(), State.SUCCESS);
            }
            found = true;
          }
        }
      }

      if (!found) {
        // TODO log error
      }
    }
  }

  public void processReceivedMessages() {
    ReceivedMessage message;

    while ((message = receivedMessages.poll()) != null) {
      if (receiveListener != null) {
        receiveListener.messageReceived(message.data, message.source);
      }
    }
  }
  
  public void processAuthAttempts() {
    ChannelIdentifier channelIdentifier;
    MessageChannel channel;
    Identifier identifier;
    
    while ((channelIdentifier = authQueue.poll()) != null) {
      channel = channelIdentifier.channel;
      identifier = channelIdentifier.identifier;
      
      if (identifier == null) {
        // Auth wasn't successfull
        identifier = channelMap.get(channel);
        // Reuse channelClosed(channel) ?
        connectionStates.put(identifier, ConnectionState.CLOSED);

        identifierMap.remove(identifier);
        channelMap.remove(channel);
      } else {
        // Auth was successfull
        connectionStates.put(identifier, ConnectionState.AUTHENTICATED);
      }
    }
  }

  @Override
  public void run() {
    // TODO Auto-generated method stub


    while (!Thread.interrupted()) {

      try {
        // TODO Problematisch?
        semaphore.acquire();
        semaphore.drainPermits();

        // Check new connections
        processNewConnections();
        
        // Check closed connections
        processClosedConnections();

        // deliver messages
        processMessageAttempts();

        // call sendListeners
        processSentMessages();

        // call receiveListeners
        processReceivedMessages();
        
        // authentication
        processAuthAttempts();

        // Check timeouts for messages

      } catch (InterruptedException ie) {
        // Do nothing
      }
    }
  }

  @Override
  public void authenticationSuccess(MessageChannel channel, Identifier identifier) {
    if (!authQueue.offer(new ChannelIdentifier(channel, identifier))) {
      // TODO log error
    }
    semaphore.release();
  }

  @Override
  public void authenticationFailed(MessageChannel channel) {
    if (!authQueue.offer(new ChannelIdentifier(channel, null))) {
      // TODO log error
    }
    semaphore.release();
  }
  
  private class ChannelIdentifier {
    public MessageChannel channel;
    public Identifier identifier;
    
    public ChannelIdentifier(MessageChannel channel, Identifier identifier) {
      this.channel = channel;
      this.identifier = identifier;
    }
  }
  
  public void setLocalIdentifier(Identifier localIdentifier) {
    this.localIdentifier = localIdentifier;
  }
}
