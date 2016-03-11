package edu.kit.tm.ptp;

import java.util.Iterator;

/**
 * Interface for classes that provide message queues for different class types.
 * 
 * @author Timon Hackenjos
 *
 */
public interface IMessageQueue {  
  /**
   * Returns true if the queue of the specified type contains a message.
   */
  <T> boolean hasMessage(Class<T> type);

  /**
   * Returns an iterator for messages of the supplied type.
   */
  <T> Iterator<QueuedMessage<T>> iterator(Class<T> type);
}
