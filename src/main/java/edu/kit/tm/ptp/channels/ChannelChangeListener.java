package edu.kit.tm.ptp.channels;

/**
 * Interface to listen for changed channels.
 *
 * @author Timon Hackenjos
 */

public interface ChannelChangeListener {
  /**
   * Gets called when a channel opened a connection successfully.
   */
  void channelOpened(MessageChannel channel);
  
  /**
   * Gets called when a channel has been closed or should be closed.
   */
  void channelClosed(MessageChannel channel);
}
