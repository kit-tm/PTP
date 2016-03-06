package edu.kit.tm.ptp;

/**
 * Container class for a message and it's source.
 * 
 * @author Timon Hackenjos
 *
 * @param <T> The type of the message.
 */
public final class QueuedMessage<T> {
  public Identifier source;
  public T data;

  public QueuedMessage(Identifier source, T data) {
    this.source = source;
    this.data = data;
  }
  
  public QueuedMessage() {
    this.source = null;
    this.data = null;
  }
}
