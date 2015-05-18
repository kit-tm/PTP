package edu.kit.tm.ptp.raw;

import edu.kit.tm.ptp.Message;


/**
 * A packet for API messages.
 *
 * @author Simeon Andreev
 *
 */
public class Packet {

	/** The message in the packet. */
	public final Message message;
	/** The status flags of the packet. */
	public final char flags;


	/**
	 * Constructor method.
	 *
	 * @param message The message in the packet.
	 * @param flags The status flags of the packet.
	 */
	public Packet(Message message, char flags) {
		this.message = message;
		this.flags = flags;
	}

}
