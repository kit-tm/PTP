package edu.kit.tm.ptp.channels;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Class.
 *
 * @author Timon Hackenjos
 */

public class ChannelManager implements Runnable {
  public ChannelManager(ChannelListener listener) {

  }
  
  public void start() {
    
  }
  
  public void stop() {
    
  }

  public void run() {

    // TODO Call finishConnect on connected channels
  }
  
  public void addServerSocket(ServerSocketChannel server) {
    
  }

  public MessageChannel connect(SocketChannel socket) {
    return null;
  }
  
  public void addChannel(MessageChannel channel) {
    
  }

  public void removeChannel(MessageChannel channel) {

  }

  public void setChannelListener(ChannelListener listener) {

  }
}
