package edu.kit.tm.ptp.serialization;

/**
 * Class.
 *
 * @author Timon Hackenjos
 */
public class ByteArrayMessage {
  private byte[] data;
  
  public ByteArrayMessage() {
    this.data = null;
  }
  
  public ByteArrayMessage(byte[] data) {
    this.data = data;
  }

  public byte[] getData() {
    return data;
  }
}
