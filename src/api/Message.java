package api;


/**
 * A wrapper for API messages.
 *
 * @author Simeon Andreev
 *
 */
public class Message {

	/** The user defined identifier of the message. */
	public final long identifier;
	/** The content of the message. */
	public final String content;
	/** The Tor hidden service destination identifier. */
	public final Identifier destination;


	/**
	 * Constructor method. Initializes the message id to 0.
	 *
	 * @param content The content of the message.
	 * @param destination The Tor hidden service destination identifier.
	 */
	public Message(String content, Identifier destination) {
		this.identifier = 0;
		this.content = content;
		this.destination = destination;
	}

	/**
	 * Constructor method.
	 *
	 * @param id The user defined identifier of the message.
	 * @param content The content of the message.
	 * @param destination The Tor hidden service destination identifier.
	 */
	public Message(long identifier, String content, Identifier destination) {
		this.identifier = identifier;
		this.content = content;
		this.destination = destination;
	}

}
