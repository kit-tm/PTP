package edu.kit.tm.ptp.serialization;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

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
 
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
      justification = "ByteArrayMessage is just a simple container."
          + " Avoid to copy data several times.")
  public ByteArrayMessage(byte[] data) {
    this.data = data;
  }

  /**
   * Returns the containing bytes.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP",
      justification = "ByteArrayMessage is just a simple container."
          + " Avoid to copy data several times.")
  public byte[] getData() {
    if (data == null) {
      throw new IllegalStateException();
    }
    
    return data;
  }
}
