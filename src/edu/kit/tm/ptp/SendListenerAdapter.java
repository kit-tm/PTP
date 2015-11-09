package edu.kit.tm.ptp;



/**
 * A convenience adapter for the SendListener class.
 *
 * @author Simeon Andreev
 *
 * @see SendListener
 */
public class SendListenerAdapter implements SendListener {


  @Override
  public void sendSuccess(Message message) {}

  @Override
  public void sendFail(Message message, FailState state) {}

}
