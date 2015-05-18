package edu.kit.tm.ptp.raw;

import edu.kit.tm.ptp.Message;
import edu.kit.tm.ptp.SendListener;



/**
 * A listener that will be notified of a message sending attempt.
 *
 * @author Simeon Andreev
 *
 */
public interface DispatchListener {

	/**
	 * Notifies this listener that a message sending should be attempted.
	 *
	 * @param message The message to send.
	 * @param lisener The listener to be notified of sending events.
	 * @param timeout The timeout for the sending.
	 * @param elapsed The amount of time the message has waited so far.
	 * @return false if a further attempt to send the message should be done.
	 */
	public boolean dispatch(Message message, SendListener lisener, long timeout, long elapsed);

}
