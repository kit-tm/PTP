package edu.kit.tm.ptp.channels;

/**
 * Interface.
 *
 * @author Timon Hackenjos
 */

public interface ChannelListener {
  void messageSent(long id, MessageChannel destination);

  void messageReceived(byte[] data, MessageChannel source);
  
  void channelOpened(MessageChannel channel);
  
  void channelClosed(MessageChannel channel);
}
