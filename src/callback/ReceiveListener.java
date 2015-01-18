package callback;


/**
 * An interface for subscribers to received messages via the local Tor hidden service socket.
 *
 * @author Simeon Andreev
 *
 */
public interface ReceiveListener {

	// TODO: change to string instead of byte array?


	/**
	 * Indicates that a message was received over the Tor Hidden Service to this listener.
	 *
	 * @param message The received message.
	 */
	public void receivedMessage(byte[] message);

}
