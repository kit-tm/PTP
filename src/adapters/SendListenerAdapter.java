package adapters;

import callback.SendListener;
import api.Message;



/**
 * A convenience adapter for the SendListener class.
 *
 * @author Simeon Andreev
 *
 * @see SendListener
 */
public class SendListenerAdapter implements SendListener {


	/**
	 * @see SendListener
	 */
	public void connectionSuccess(Message message) {}

	/**
	 * @see SendListener
	 */
	public void connectionTimeout(Message message) {}

	/**
	 * @see SendListener
	 */
	public void sendSuccess(Message message) {}

	/**
	 * @see SendListener
	 */
	public void sendFail(Message message) {}

}
