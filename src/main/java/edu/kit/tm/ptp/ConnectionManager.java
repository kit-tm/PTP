package edu.kit.tm.ptp;

import java.nio.channels.SocketChannel;
import java.util.Map;

/**
 * Class.
 *
 * @author Timon Hackenjos
 */
public class ConnectionManager {
  private Map<Identifier, SocketChannel> identifierMap;
  private Map<SocketChannel, Identifier> channelMap;
  
  public void start() {
    
  }
  
  public void stop() {
    
  }

  long send(byte[] data, Identifier destination) {
    return 0;
  }

  public void addSendListener(SendListener listener) {

  }

  public void addReceiveListener(ReceiveListener listener) {

  }

  public void connect(Identifier destination) {

  }
  
  public void disconnect(Identifier destination) {
    
  }
  
  public int startBindServer() {
    return 0;
  }
  

}
