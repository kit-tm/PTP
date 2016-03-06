package edu.kit.tm.ptp;

/**
 * Interface for classes that provide message queues for different class types.
 * 
 * @author Timon Hackenjos
 *
 */
public interface IMessageQueue {

  /**
   * Polls a message from the queue of the specified type.
   * The returned object is the same as the QueuedMessage passed as parameter.
   * The QueuedMessage object also contains the source of the message.
   * 
   * @param type The class type that determines the queue to poll from.
   * @param message Object that gets filled with the message itself and the source.
   * @return The QueuedMessage passed as argument or null if the queue is currently empty.
   */
  <T> QueuedMessage<T> pollMessage(Class<T> type, QueuedMessage<T> message);
  
  /**
   * Poll a message from the queue of the specified type.
   * 
   * @param type The class type that determines the queue to poll from.
   * @return The message or null if the queue is currently empty.
   */
  <T> T pollMessage(Class<T> type);
  
  /**
   * Returns the source of a previously received message.
   */
  Identifier getMessageSource(Object message);
  
  /**
   * Returns true if the queue of the specified type contains a message.
   */
  <T> boolean hasMessage(Class<T> type);
}
