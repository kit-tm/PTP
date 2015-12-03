package edu.kit.tm.ptp.channels;

import java.nio.channels.SocketChannel;

/**
 * Interface.
 *
 * @author Timon Hackenjos
 */

public interface ChannelListener {
  void messageSend(long id, SocketChannel destination);

  void messageReceived(byte[] data, SocketChannel source);
}
