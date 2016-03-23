package edu.kit.tm.ptp;

/**
 * Generic queue which can only be polled.
 * 
 * @author Timon Hackenjos
 *
 * @param <T> The type of the items in the queue.
 */
public class MessageQueue<T> implements IMessageQueue<T> {
  private Class<T> type;
  private MessageQueueContainer container;
  
  /**
   * Constructs a new MessageQueue object.
   * 
   * @param type The type of items to poll.
   * @param container The container to poll items from.
   */
  public MessageQueue(Class<T> type, MessageQueueContainer container) {
    this.type = type;
    this.container = container;
  }

  @Override
  public boolean hasMessage() {
    return container.hasMessage(type);
  }

  @Override
  public QueuedMessage<T> pollMessage() {
    return container.pollMessage(type);
  }

}
