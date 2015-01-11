package api;

import callback.ReceiveListener;


/**
 * This class assembles messages which are broken down or combined due to the socket sending.
 *
 * @author Simeon Andreev
 *
 */
public class MessageAssembler implements ReceiveListener {

	/** A delimiter to separate message length from message content. */
	private static final String messageLengthDelimiter = ".";

	/** Buffer in which messages are assembled. */
	private StringBuilder messageBuilder = new StringBuilder("");
	/** The length of the message which is currently being assembled */
	private int messageLength = 0;
	/** The listener to which assembled messages will be delegated. */
	private ReceiveListener listener = new ReceiveListener() {

		@Override
		public void receive(byte[] message) { /* Do nothing. */ }

	};


	/**
	 * Constructor method.
	 */
	public MessageAssembler() { }


	/**
	 * @see ReceiveListener
	 */
	@Override
	public void receive(byte[] bytes) {
		// Wrap the received fragment.
		final String fragment = new String(bytes);

		// Add the received fragment to the current message.
		messageBuilder.append(fragment);

		// Check if we can complete the current message and possibly further messages.
		while (messageBuilder.length() >= messageLength) {
			// Check if the current message size is not set.
			if (messageLength == 0) {
				final int position = messageBuilder.indexOf(messageLengthDelimiter);
				// If the buffer contains the next messages length, fetch and remove it from the buffer.
				if (position != -1) {
					messageLength = Integer.valueOf(messageBuilder.substring(0, position));
					messageBuilder.replace(0, position + 1, "");
				// Otherwise do nothing until the next fragment is received.
				} else
					break;
			// Otherwise check if
			} else {
				// If so, delegate the received message and remove it from the buffer.
				listener.receive(messageBuilder.substring(0, messageLength).getBytes());
				messageBuilder.replace(0, messageLength, "");
				messageLength = 0;
			}
		}
	}


	/**
	 * Sets the listener to which assembled messages will be delegated.
	 *
	 * @param listener The listener to which assembled messages will be delegated.
	 */
	public void setListener(ReceiveListener listener) { this.listener = listener; }


	/**
	 * Alters the content of a message to enable the assembly of messages from received fragments.
	 *
	 * @param content The message which should be altered.
	 * @return The altered content.
	 */
	public String alterContent(String content) {
		// Add the message content length to the actual content.
		return content.length() + messageLengthDelimiter + content;
	}

}