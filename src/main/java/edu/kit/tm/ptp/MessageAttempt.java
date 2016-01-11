package edu.kit.tm.ptp;

public class MessageAttempt {

  private long id;
  private long sendTimestamp;
  private byte[] data;
  private long timeout;
  private Identifier destination;
  private boolean registered;
  
  public MessageAttempt(long id, long sendTimestamp, byte[] data, long timeout,
      Identifier destination) {
    setId(id);
    setSendTimestamp(sendTimestamp);
    setData(data);
    setTimeout(timeout);
    setDestination(destination);
    setRegistered(false);
  }
  
  public boolean isRegistered() {
    return registered;
  }

  public void setRegistered(boolean registered) {
    this.registered = registered;
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
  
  public byte[] getData() {
    return data;
  }
  
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
