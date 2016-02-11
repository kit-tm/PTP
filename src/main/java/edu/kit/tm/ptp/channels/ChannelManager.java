package edu.kit.tm.ptp.channels;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Coordinates reading from and writing to several MessageChannels.
 *
 * @see MessageChannel
 * @author Timon Hackenjos
 */

public class ChannelManager implements Runnable {
  private Selector selector = null;
  private ChannelListener listener;
  private Thread thread;
  private static final Logger logger = Logger.getLogger(ChannelManager.class.getName());

  /**
   * Initializes a new ChannelManager.
   * 
   * @param listener The ChannelListener to inform about changed channels and messages.
   */
  public ChannelManager(ChannelListener listener) {
    if (listener == null) {
      throw new NullPointerException();
    }

    this.listener = listener;
    thread = new Thread(this);
    selector = null;
  }

  /**
   * Starts a thread to handle reading an writing.
   */
  public void start() throws IOException {
    selector = Selector.open();
    thread.start();
  }

  /**
   * Stops a previously started thread. Does nothing if the thread has been stopped before.
   */
  public void stop() {
    thread.interrupt();
    try {
      // Does nothing if thread isn't running
      thread.join();
    } catch (InterruptedException e) {
      logger.log(Level.WARNING, "Failed to wait for thread to stop: " + e.getMessage());
    }
    
    try {
      if (selector != null) {
        selector.close();
      }
    } catch (IOException e) {
      logger.log(Level.WARNING, "Failed to close selector: " + e.getMessage());
    }
  }

  @Override
  public void run() {

    int readyChannels = 0;
    long timeout = 100;

    while (!thread.isInterrupted()) {
      try {
        readyChannels = selector.select(timeout);
      } catch (IOException e) {
        logger.log(Level.WARNING, "Error occurred during selection operation: " + e.getMessage());
        thread.interrupt();
        continue;
      }

      if (readyChannels == 0) {
        continue;
      }

      Set<SelectionKey> selectedKeys = selector.selectedKeys();
      Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

      while (keyIterator.hasNext()) {
        SelectionKey key = keyIterator.next();

        if (key.isValid() && key.isAcceptable()) {
          ServerSocketChannel server = (ServerSocketChannel) key.attachment();
          try {
            SocketChannel client = server.accept();
            client.configureBlocking(false);
            MessageChannel channel = new MessageChannel(client, this);
            listener.channelOpened(channel);
          } catch (IOException e) {
            logger.log(Level.WARNING,
                "Caught exception while accepting connection: " + e.getMessage());
          }

        } else {
          MessageChannel channel = (MessageChannel) key.attachment();

          if (key.isValid() && key.isConnectable()) {
            try {
              // unregister channel
              key.interestOps(0);
              
              if (channel.getChannel().finishConnect()) {
                listener.channelOpened(channel);
              } else {
                key.cancel();
                listener.channelClosed(channel);
              }
            } catch (IOException ioe) {
              logger.log(Level.WARNING,
                  "Caught exception while handling connectable channel: " + ioe.getMessage());
              key.cancel();
              listener.channelClosed(channel);
            }
          }

          if (key.isValid() && key.isReadable()) {
            channel.read();
          }

          if (key.isValid() && key.isWritable()) {
            channel.write();
          }
        }

        keyIterator.remove();
      }

    }

  }

  /**
   * Adds a ServerSocketChannel to accept connections from.
   * The server has to be listening already.
   * Calls channelOpened() on the ChannelListener when a
   * connection is received.
   * 
   * @param server The ServerSocketChannel to accept connections from.
   * @throws IOException If it fails to register the server.
   * @see ChannelListener
   */
  public void addServerSocket(ServerSocketChannel server) throws IOException {
    server.configureBlocking(false);
    server.register(selector, SelectionKey.OP_ACCEPT, server);
  }

  /**
   * Adds a SocketChannel which should be connected.
   * The connect() method of the SocketChannel has to be called already.
   * Calls channelOpened() on the ChannelListener if the connection
   * attempt was successful.
   * 
   * @param socket The SocketChannel to connect.
   * @return A MessageChannel to be able to read and write later on.
   * @throws IOException If it fails to register the channel.
   */
  public MessageChannel connect(SocketChannel socket) throws IOException {
    socket.configureBlocking(false);
    MessageChannel channel = new MessageChannel(socket, this);
    socket.register(selector, SelectionKey.OP_CONNECT, channel);
    return channel;
  }

  /**
   * Adds MessageChannel to the manager.
   * Reading and writing needs to be enabled separately.
   * 
   * @param channel The MessageChannel. 
   * @throws ClosedChannelException If the channel is closed.
   */
  public void addChannel(MessageChannel channel) throws ClosedChannelException {
    channel.getChannel().register(selector, 0, channel);
  }

  /**
   * Stops to read from and write messages to the supplied MessageChannel.
   */
  public void removeChannel(MessageChannel channel) {
    SelectionKey key = channel.getChannel().keyFor(selector);

    if (key != null) {
      key.cancel();
    }
  }

  /**
   * Returns the current ChannelListener.
   */
  public ChannelListener getChannelListener() {
    return listener;
  }

  /**
   * Tells the ChannelManager if the supplied channel has data to write.
   */
  public void registerWrite(MessageChannel channel, boolean enable) {
    setInterestOps(channel, enable, SelectionKey.OP_WRITE);
  }
  
  /**
   * Tells the ChannelManager if the supplied channel is ready to read data.
   */
  public void registerRead(MessageChannel channel, boolean enable) {
    setInterestOps(channel, enable, SelectionKey.OP_READ);
  }
  
  private void setInterestOps(MessageChannel channel, boolean enable, int operation) {
    SelectionKey key = channel.getChannel().keyFor(selector);

    if (key == null) {
      // Can happen for incoming connections
      logger.log(Level.INFO, "Unregistered channel tries to register operation.");
      return;
    }
    
    if (!key.isValid()) {
      logger.log(Level.INFO, "Closed or unregistered channel tries to register operation.");
      return;
    }

    if (enable) {
      key.interestOps(key.interestOps() | operation);
    } else {
      key.interestOps(key.interestOps() & (~operation));
    }
  }
}
