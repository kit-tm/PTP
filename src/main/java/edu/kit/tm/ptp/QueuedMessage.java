package edu.kit.tm.ptp;

/**
 * Container class for a message, it's source
 * and the time the message was received.
 * 
 * @author Timon Hackenjos
 *
 * @param <T> The type of the message.
 */
public final class QueuedMessage<T> {
  public Identifier source = null;
  public T data = null;
  long receiveTime = -1;

  public QueuedMessage(Identifier source, T data) {
    this.source = source;
    this.data = data;
  }
  
  public QueuedMessage(Identifier source, T data, long receiveTime) {
    this(source, data);
    this.receiveTime = receiveTime;
  }
  
  public QueuedMessage() {    
  }
}
