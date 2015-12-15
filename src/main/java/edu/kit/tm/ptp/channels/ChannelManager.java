package edu.kit.tm.ptp.channels;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * Class.
 *
 * @author Timon Hackenjos
 */

public class ChannelManager implements Runnable {
  private Selector selector;
  private ChannelListener listener;
  private Thread thread;

  public ChannelManager(ChannelListener listener) {
    if (listener == null) {
      throw new NullPointerException();
    }

    this.listener = listener;
    thread = new Thread(this);
    selector = null;
  }

  public void start() throws IOException {
    selector = Selector.open();
    thread.start();
  }

  public void stop() throws IOException {
    thread.interrupt();
    selector.close();
  }

  public void run() {
    
    int readyChannels = 0;
    long timeout = 100;

    while (!thread.isInterrupted()) {
      try {
        readyChannels = selector.select(timeout);
      } catch (IOException e1) {
        thread.interrupt();
        // TODO log error
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
            // TODO Auto-generated catch block
            e.printStackTrace();
          }

        } else {
          MessageChannel channel = (MessageChannel) key.attachment();

          if (key.isValid() && key.isConnectable()) {
            try {
              channel.getChannel().finishConnect();
              //key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
              listener.channelOpened(channel);
            } catch (IOException ioe) {
              // TODO log error
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

  public void addServerSocket(ServerSocketChannel server) throws IOException {
    server.configureBlocking(false);
    server.register(selector, SelectionKey.OP_ACCEPT, server);
  }

  public MessageChannel connect(SocketChannel socket) throws IOException {
    socket.configureBlocking(false);
    MessageChannel channel = new MessageChannel(socket, this);
    socket.register(selector, SelectionKey.OP_CONNECT, channel);
    return channel;
  }

  public void addChannel(MessageChannel channel) throws ClosedChannelException {
    channel.getChannel().register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, channel);
  }

  public void removeChannel(MessageChannel channel) {
    SelectionKey key = channel.getChannel().keyFor(selector);

    if (key == null) {
      throw new IllegalArgumentException();
    }

    key.cancel();
    
    channel.closeChannel();
  }

  public ChannelListener getChannelListener() {
    return listener;
  }
}
