package api;


/**
 * A wrapper for API messages.
 *
 * @author Simeon Andreev
 *
 */
public class Message {

	/** The content of the message. */
	public final String content;
	/** The Tor hidden service destination identifier. */
	public final Identifier identifier;


	/**
	 * Constructor method.
	 *
	 * @param content The content of the message.
	 * @param destination The Tor hidden service destination identifier.
	 */
	public Message(String content, Identifier identifier) {
		this.content = content;
		this.identifier = identifier;
	}

}
