package edu.kit.tm.ptp.serialization;

/**
 * Class.
 *
 * @author Timon Hackenjos
 */
public class Message {
  private byte[] data;
  
  public Message(byte[] data) {
    this.data = data;
  }

  public byte[] getData() {
    return data;
  }
}
