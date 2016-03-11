package edu.kit.tm.ptp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Extends ListenerContainer to allow to save queued messages of
 * previously registered types.
 * 
 * @author Timon Hackenjos
 */

public class MessageQueueContainer extends ListenerContainer {
  private Map<Class<?>, Queue<Object>> queues = new HashMap<Class<?>, Queue<Object>>();
  
  /**
   * Adds a queue for messages of Type type.
   */
  public <T> void addMessageQueue(Class<T> type) {
    if (queues.get(type) != null) {
      throw new IllegalArgumentException();
    }
     
    queues.put(type, new ConcurrentLinkedQueue<Object>());
    registerClasses.add(type);
  }
  
  /**
   * Returns a message of the supplied type or null if the queue is empty.
   */
  protected <T> QueuedMessage<T> pollMessage(Class<T> type) {
    Queue<Object> queue = queues.get(type);
    
    if (queue == null) {
      throw new IllegalArgumentException("Type hasn't been registered before.");
    }
    
    Object obj = queue.poll();
    
    if (obj == null) {
      return null;
    }
    
    @SuppressWarnings("unchecked")
    QueuedMessage<T> objT = (QueuedMessage<T>) obj;
    
    return objT;
  }

  /**
   * Returns true if the queue of the specified type contains a message.
   */
  protected <T> boolean hasMessage(Class<T> type) {
    Queue<Object> queue = queues.get(type);
    
    if (queue == null) {
      throw new IllegalArgumentException("Type hasn't been registered before.");
    }
    
    return queue.size() > 0;
  }

  /**
   * Adds a message to the corresponding queue by determining it's type
   * by comparing it's type to the registered types.
   * 
   * @param message The message to add.
   * @param source The source of the message.
   */
  public void addMessageToQueue(Object message, Identifier source, long receiveTime) {
    addMessage(getType(message).cast(message), source, receiveTime);
  }
  
  /**
   * Returns true if it exists a queue for the supplied message.
   */
  public boolean hasQueue(Object message) {
    Class<?> type = getTypeOrNull(message);
    if (type == null) {
      return false;
    }
    
    return queues.get(type) != null;
  }
  
  private <T> void addMessage(T message, Identifier source, long receiveTime) {
    Queue<Object> queue = queues.get(message.getClass());
    
    if (queue == null) {
      throw new IllegalArgumentException("Type of object hasn't been registered before");
    }
    
    queue.add(new QueuedMessage<T>(source, message, receiveTime));
  }

  /**
   * Returns an iterator for messages of the supplied type.
   */
  public <T> Iterator<QueuedMessage<T>> iterator(Class<T> type) {
    return new MessageQueueIterator<T>(type, this);
  }
}
