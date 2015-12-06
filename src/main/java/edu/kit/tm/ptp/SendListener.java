package edu.kit.tm.ptp;



/**
 * An interface for subscribers to message sending notifications.
 *
 * @author Simeon Andreev
 *
 */
public interface SendListener {


  /**
   * A state of sending failure enumeration. * CONNECTION_TIMEOUT indicates a connection timeout. *
   * SEND_TIMEOUT indicates a sending timeout, although a connection was established.
   *
   * @author Simeon Andreev
   *
   */
  public enum State {
    SUCCESS, CONNECTION_TIMEOUT, SEND_TIMEOUT
  }

  /**
   * Indicates that the message could not be sent.
   *
   * @param message The message with which the sending was initiated.
   * @param state The reason why the sending failed.
   */
  public void messageSent(long id, Identifier destination, State state);

}
