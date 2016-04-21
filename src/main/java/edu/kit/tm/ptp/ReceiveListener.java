package edu.kit.tm.ptp;



/**
 * An interface for subscribers to received byte[] messages.
 *
 * @author Simeon Andreev
 *
 */
public interface ReceiveListener {


  /**
   * Indicates that a message was received.
   *
   * @param data The received message.
   * @param source The hidden service identifier of the source of the message.
   */
  public void messageReceived(byte[] data, Identifier source);

}
