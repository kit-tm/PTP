package edu.kit.tm.ptp.connection;

import edu.kit.tm.ptp.Identifier;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * An attempt to send a message.
 * 
 * @author Timon Hackenjos
 *
 */
public class MessageAttempt {

  private long id;
  private long sendTimestamp;
  private byte[] data;
  private long timeout;
  private Identifier destination;
  private boolean informSendListener;

  /**
   * Constructs a new MessageAttempt.
   * 
   * @param id The identifier to use when reporting the result of the sending.
   * @param sendTimestamp The timestamp of the attempt.
   * @param data The data to send.
   * @param timeout How long to wait before the attempt times out.
   * @param destination The destination of the message.
   */
  public MessageAttempt(long id, long sendTimestamp, byte[] data, long timeout,
      Identifier destination, boolean informSendListener) {
    setId(id);
    setSendTimestamp(sendTimestamp);
    setData(data);
    setTimeout(timeout);
    setDestination(destination);
    setInformSendListener(informSendListener);
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }


  public long getSendTimestamp() {
    return sendTimestamp;
  }

  public void setSendTimestamp(long sendTimestamp) {
    this.sendTimestamp = sendTimestamp;
  }

  @SuppressFBWarnings(value = "EI_EXPOSE_REP",
      justification = "MessageAttempt is just a simple container."
          + " Avoid to copy data several times.")
  public byte[] getData() {
    return data;
  }

  @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
      justification = "MessageAttempt is just a simple container."
          + " Avoid to copy data several times.")
  public void setData(byte[] data) {
    this.data = data;
  }

  public long getTimeout() {
    return timeout;
  }

  public void setTimeout(long timeout) {
    this.timeout = timeout;
  }

  public Identifier getDestination() {
    return destination;
  }

  public void setDestination(Identifier destination) {
    this.destination = destination;
  }
  
  public boolean isInformSendListener() {
    return informSendListener;
  }

  public void setInformSendListener(boolean informSendListener) {
    this.informSendListener = informSendListener;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (id ^ (id >>> 32));
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null) {
      return false;
    }

    if (getClass() != obj.getClass()) {
      return false;
    }

    MessageAttempt other = (MessageAttempt) obj;

    if (id != other.id) {
      return false;
    }

    return true;
  }
}
