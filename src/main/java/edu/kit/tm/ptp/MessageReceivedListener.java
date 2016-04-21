package edu.kit.tm.ptp;

/**
 * Interface for subscribers to received objects.
 * 
 * @param T The type of the objects to receive.
 *
 * @author Timon Hackenjos
 */
public interface MessageReceivedListener<T> {
  /**
   * Indicates that a message was received.
   * 
   * @param message The received message.
   * @param source The hidden service identifier of the source of the message.
   */
  void messageReceived(T message, Identifier source);
}
