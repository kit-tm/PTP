package edu.kit.tm.ptp.serialization;

/**
 * Message which consists of a byte array.
 *
 * @author Timon Hackenjos
 */
public class ByteArrayMessage {
  private byte[] data;
  
  /**
   * Constructor is needed to be able to serialize an object.
   */
  public ByteArrayMessage() {
    this.data = null;
  }
  
  public ByteArrayMessage(byte[] data) {
    this.data = data;
  }

  /**
   * Returns the containing bytes.
   */
  public byte[] getData() {
    if (data == null) {
      throw new IllegalStateException();
    }
    
    return data;
  }
}
