package callback;

import api.Message;


/**
 * An interface for subscribers to received messages via the local Tor hidden service socket.
 *
 * @author Simeon Andreev
 *
 */
public interface ReceiveListener {


	/**
	 * Indicates that a message was received over the Tor Hidden Service.
	 *
	 * @param message The received message.
	 */
	public void receivedMessage(Message message);

}
