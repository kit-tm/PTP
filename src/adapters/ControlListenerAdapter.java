package adapters;

import callback.ControlListener;


/**
 * A convenience adapter for the ControlListener class.
 *
 * @author Simeon Andreev
 *
 * @see ControlListener
 */
public class ControlListenerAdapter implements ControlListener {


	/**
	 * @see ControlListener
	 */
	@Override
	public void receivedMessage(char flag, String message) { }

}
