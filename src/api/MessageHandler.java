package api;

import utility.Constants;
import callback.ReceiveListener;
import callback.ReceiveListenerAdapter;


/**
 * This class handles messages received via the API.
 * Messages are assumed to be at most glued during unwrapping,
 * i.e. bulk messages are supported, fragmentation is not.
 *
 * @author Simeon Andreev
 *
 */
public class MessageHandler implements ReceiveListener {

	/** The listener to notify when control messages are received. */
	private final ReceiveListener controlListener;
	/** The listener to notify when standard messages are received. */
	private ReceiveListener standardListener = new ReceiveListenerAdapter();


	/**
	 * Constructor method.
	 *
	 * @param controlListener The listener to notify when control messages are received.
	 */
	public MessageHandler(ReceiveListener controlListener) {
		this.controlListener = controlListener;
	}


	/**
	 * Set the listener to notify when standard messages are received.
	 *
	 * @param standardListener The listener to notify when standard messages are received.
	 */
	public void setStandardListener(ReceiveListener standardListener) {
		this.standardListener = standardListener;
	}


	/**
	 * Removes the meta information from a message bulk and
	 * extracts the messages in the bulk. Notifies the
	 * control and standard message listeners.
	 *
	 * @param bytes The message bulk bytes.
	 *
	 * @see ReceiveListener
	 */
	@Override
	public void receivedMessage(byte[] bytes) {
		final String bulk = new String(bytes);
		int index = 0;

		// Check if we can complete the current message and possibly further messages.
		while (true) {
			// Get the position of the next wrapped message length delimiter.
			final int position = bulk.indexOf(Constants.messagelengthdelimiter, index);
			// If no length delimiter is found we are done.
			if (position == -1) return;
			// Fetch the message length.
			final int length = Integer.valueOf(bulk.substring(index, position));
			// Fetch the message flag.
			final char flag = bulk.charAt(position + 1);
			// Fetch the message.
			final String message = bulk.substring(position + 2, position + 2 + length);
			// Delegate the message to the respective listener.
			if (flag == Constants.messagecontrolflag)
				controlListener.receivedMessage(message.getBytes());
			else if (flag == Constants.messagestandardflag)
				standardListener.receivedMessage(message.getBytes());
			// Move to the next message.
			index = position + 2 + length;
		}
	}


	/**
	 * Wraps a message with meta information.
	 *
	 * @param message The message which should be wrapped.
	 * @return The wrapped content.
	 */
	public String wrap(String message) {
		// Add the message content length to the actual content.
		return message.length() + Constants.messagelengthdelimiter + Constants.messagestandardflag + message;
	}

}