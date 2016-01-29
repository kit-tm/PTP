package edu.kit.tm.ptp.channels;

/**
 * Interface to listen for changed channels and sent and received messages.
 *
 * @author Timon Hackenjos
 */

public interface ChannelListener {
  /** 
   * Gets called when the message with the id has been sent successfully.
   * 
   * @param id The id of the message as returned by send().
   * @param destination The destination of the message.
   */
  void messageSent(long id, MessageChannel destination);

  /**
   * Gets called when a new message has been received.
   * 
   * @param data The message itself.
   * @param source The channel that received the message.
   */
  void messageReceived(byte[] data, MessageChannel source);
  
  /**
   * Gets called when a channel opened a connection successfully.
   */
  void channelOpened(MessageChannel channel);
  
  /**
   * Gets called when a channel has been closed or should be closed.
   */
  void channelClosed(MessageChannel channel);
}
