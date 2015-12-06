package edu.kit.tm.ptp;



/**
 * A convenience adapter for the ReceiveListener class.
 *
 * @author Simeon Andreev
 *
 * @see ReceiveListener
 */
public class ReceiveListenerAdapter implements ReceiveListener {


  @Override
  public void messageReceived(byte[] data, Identifier source) {}

}
