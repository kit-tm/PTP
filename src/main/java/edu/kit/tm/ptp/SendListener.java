package edu.kit.tm.ptp;



/**
 * An interface for subscribers to message sending notifications.
 *
 * @author Simeon Andreev
 * @author Timon Hackenjos
 *
 */
public interface SendListener {


  /**
   * Describes the state of a message which should be sent.
   * 
   * <li>{@link #SUCCESS}</li>
   * <li>{@link #TIMEOUT}</li>
   * <li>{@link #INVALID_DESTINATION}</li>
   *
   * @author Simeon Andreev
   * @author Timon Hackenjos
   *
   */
  public enum State {
    /** The message has been sent successfully. */
    SUCCESS, 
    /** The attempt to send the message timed out. */
    TIMEOUT, 
    /** The destination is invalid. */
    INVALID_DESTINATION
  }

  /**
   * Indicates that the message was sent or could not be sent.
   *
   * @param id The id of the message.
   * @param destination The destination of the message.
   * @param state If the sending succeeded and why.
   */
  public void messageSent(long id, Identifier destination, State state);

}
