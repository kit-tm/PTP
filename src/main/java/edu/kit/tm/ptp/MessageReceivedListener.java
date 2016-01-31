package edu.kit.tm.ptp;

/**
 * Interface for subscribers to received objects.
 *
 * @author Timon Hackenjos
 */
public interface MessageReceivedListener<T> {
  void messageReceived(T message, Identifier source);
}
