package edu.kit.tm.ptp;

import java.util.Iterator;

/**
 * Generic Iterator for a messages of type T.
 * 
 * @author Timon Hackenjos
 *
 * @param <T> The type of the messages to iterate over.
 */
public class MessageQueueIterator<T> implements Iterator<QueuedMessage<T>> {
  private Class<T> type;
  private MessageQueueContainer container;
  
  /**
   * Constructs a new object.
   * 
   * @param type The type of messages to return.
   * @param container The MessageQueueContainer to poll messages from.
   */
  public MessageQueueIterator(Class<T> type, MessageQueueContainer container) {
    this.type = type;
    this.container = container;
  }
  
  @Override
  public boolean hasNext() {
    return container.hasMessage(type);
  }

  @Override
  public QueuedMessage<T> next() {
    return container.pollMessage(type);
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

}
