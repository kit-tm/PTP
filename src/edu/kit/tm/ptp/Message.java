package edu.kit.tm.ptp;


/**
 * A wrapper for API messages.
 *
 * @author Simeon Andreev
 *
 */
public class Message {

	/** The id of the message */
	public final long id;
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
		this.id = 0;
		this.content = content;
		this.identifier = identifier;
	}

	/**
	 * Constructor method.
	 *
	 * @param id The id of the message.
	 * @param content The content of the message.
	 * @param destination The Tor hidden service destination identifier.
	 */
	public Message(long id, String content, Identifier identifier) {
		this.id = id;
		this.content = content;
		this.identifier = identifier;
	}


	/**
	 * @see Object
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Message)) return false;
		if (obj == this) return true;
		Message msg = (Message)obj;
		return id == msg.id && content.equals(msg.content) && identifier.equals(msg.identifier);
	}

}
