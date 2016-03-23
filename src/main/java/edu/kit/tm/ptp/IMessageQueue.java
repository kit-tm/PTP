package edu.kit.tm.ptp;

/**
 * Interface for a generic queue which can only be polled.
 * 
 * @author Timon Hackenjos
 *
 * @param <T> The type of the items in the queue.
 */
public interface IMessageQueue<T> {
  /**
   * Returns true if the queue contains a message.
   */
  boolean hasMessage();
  
  /**
   * Return the next item in the queue or null if the queue is empty.
   */
  QueuedMessage<T> pollMessage();
}
