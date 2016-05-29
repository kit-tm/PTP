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
  private Identifier source = null;
  private T data = null;
  private long receiveTime = -1;

  protected QueuedMessage(Identifier source, T data) {
    this.source = source;
    this.data = data;
  }
  
  protected QueuedMessage(Identifier source, T data, long receiveTime) {
    this(source, data);
    this.receiveTime = receiveTime;
  }
  
  public QueuedMessage() {    
  }
  
  public Identifier getSource() {
    return source;
  }

  public T getData() {
    return data;
  }

  public long getReceiveTime() {
    return receiveTime;
  }
}
