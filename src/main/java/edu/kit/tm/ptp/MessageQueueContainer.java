package edu.kit.tm.ptp;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Extends ListenerContainer to allow to save queued messages of
 * previously registered types.
 * 
 * @author Timon Hackenjos
 */

public class MessageQueueContainer extends ListenerContainer implements IMessageQueue {
  private Map<Class<?>, Queue<Object>> queues = new HashMap<Class<?>, Queue<Object>>();
  private Map<Object, Identifier> sources = new HashMap<Object, Identifier>();
  
  /**
   * Adds a queue for messages of type type.
   */
  public <T> void addMessageQueue(Class<T> type) {
    if (queues.get(type) != null) {
      throw new IllegalArgumentException();
    }
     
    queues.put(type, new ConcurrentLinkedQueue<Object>());
    registerClasses.add(type);
  }
  
  @Override
  public <T> T pollMessage(Class<T> type) {
    Queue<Object> queue = queues.get(type);
    
    if (queue == null) {
      throw new IllegalArgumentException("Type hasn't been registered before.");
    }
    
    Object obj = queue.poll();
    
    if (obj == null) {
      return null;
    }
    
    @SuppressWarnings("unchecked")
    T objT = (T) obj;
    
    return objT;
  }
  
  @Override
  public <T> QueuedMessage<T> pollMessage(Class<T> type, QueuedMessage<T> message) {
    T messageT = pollMessage(type);
    
    if (messageT == null) {
      return null;
    }
    
    message.data = messageT;
    message.source = getMessageSource(message.data);
    return message;
  }
  
  @Override
  public Identifier getMessageSource(Object message) {
    if (message == null) {
      throw new IllegalArgumentException();
    }
    
    Identifier source = sources.get(message);
    
    if (source != null) {
      sources.remove(message);
    }
    
    return source;
  }

  @Override
  public <T> boolean hasMessage(Class<T> type) {
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
  public void addMessageToQueue(Object message, Identifier source) {
    addMessage(getType(message).cast(message));
    sources.put(message, source);
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
  
  private <T> void addMessage(T message) {
    Queue<Object> queue = queues.get(message.getClass());
    
    if (queue == null) {
      throw new IllegalArgumentException("Type of object hasn't been registered before");
    }
    
    queue.add(message);
  }
  

}
