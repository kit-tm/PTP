package callback;

import api.Message;


/**
 * An interface for subscribers to message sending notifications.
 *
 * @author Simeon Andreev
 *
 */
public interface SendListener {


	/**
	 * Indicates that a connection was established.
	 *
	 * @param message The message with which the sending was initiated.
	 */
	void connectionSuccess(Message message);

	/**
	 * Indicates that a connection was not established in the given timeout.
	 *
	 * @param message The message with which the sending was initiated.
	 */
	void connectionTimeout(Message message);

	/**
	 * Indicates that the message was sent.
	 *
	 * @param message The message with which the sending was initiated.
	 */
	void sendSuccess(Message message);

	/**
	 * Indicates that the message could not be sent.
	 *
	 * @param message The message with which the sending was initiated.
	 */
	void sendFail(Message message);

}
