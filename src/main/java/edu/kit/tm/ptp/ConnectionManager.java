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

  long send(byte[] data, Identifier destination) {
    return 0;
  }

  public void addSendListener(SendListener listener) {

  }

  public void addReceiveListener(ReceiveListener listener) {

  }

  private void connect(Identifier destination) {

  }
}
