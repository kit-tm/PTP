package callback;


/**
 * An interface for subscribers to received API control messages.
 *
 * @author Simeon Andreev
 *
 */
public interface ControlListener {


	/**
	 * Notifies the listener of a received control message.
	 *
	 * @param flag The flag of the control message.
	 * @param message The control message.
	 */
	public void receivedMessage(char flag, String message);

}
