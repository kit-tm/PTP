package receive;

import java.net.Socket;

import api.Identifier;


/**
 * A pair of a socket and an identifier.
 *
 * @author Simeon Andreev
 *
 */
public class Origin {


	/** The socket. */
	public final Socket socket;
	/** The identifier of the socket. */
	public Identifier identifier = null;


	/**
	 * Constructor method.
	 *
	 * @param address The Tor hidden service identifier of the pair.
	 * @param socket The socket of the pair.
	 */
	public Origin(String address, Socket socket) {
		this.identifier = new Identifier(address);
		this.socket = socket;
	}

}
