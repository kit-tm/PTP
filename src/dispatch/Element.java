package dispatch;

import api.Message;
import callback.SendListener;


/**
 * A wrapper class for dispatcher queue elements.
 *
 * @author Simeon Andreev
 */
public class Element {

	/** The message stored in the queue element. */
	public final Message message;
	/** The timeout for the sending. */
	public final long timeout;
	/** The listener to notify of sending events. */
	public final SendListener listener;
	/** Waiting time of the message. */
	public final long timestamp;


	/**
	 * Constructor method.
	 *
	 * @param message The message to send with the queue element.
	 * @param timeout The timeout for the sending.
	 * @param listener The listener to notify of sending events.
	 */
	public Element(Message message, long timeout, SendListener listener) {
		this.message = message;
		this.timeout = timeout;
		this.listener = listener;
		timestamp = System.currentTimeMillis();
	}

}
