package edu.kit.tm.ptp;

import edu.kit.tm.ptp.connection.ExpireListener;
import edu.kit.tm.ptp.connection.TimerManager;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class to detect broken connections. The sender of a message expects to receive an arbitrary
 * message back from the receiver in the time interval configured by ...
 * If the receiver has no regular message to send it can send an IsAliveMessage.
 * IsAliveMessages must NOT be replied to by another IsAliveMessage.
 * 
 * @author Timon Hackenjos
 *
 */
public class IsAliveManager implements ExpireListener {
  private static final int SENDTIMERCLASS = 0;
  private static final int RECEIVETIMERCLASS = 1;

  private final PTP ptp;
  private final int isAliveTimeout;
  private final int isAliveSendTimeout;

  private TimerManager timerManager;

  private static final Logger logger = Logger.getLogger(IsAliveManager.class.getName());

  public IsAliveManager(PTP ptp, Configuration config) {
    this.ptp = ptp;
    timerManager = new TimerManager(this, config.getTimerUpdateInterval());
    isAliveTimeout = config.getIsAliveTimeout();
    isAliveSendTimeout = config.getIsAliveSendTimeout();
  }
  
  /**
   * Informs the manager that a message was received.
   * 
   * @param source The identifier of the source of the message.
   * @param isAliveMsg True if the message was a keepalive.
   */
  public void messageReceived(Identifier source, boolean isAliveMsg) {
    // We received a message. Stop receive timer.
    timerManager.remove(source, RECEIVETIMERCLASS);
    
    if (!isAliveMsg) {
      // It's not a isAliveMessage so we have to answer it. Set timer
      timerManager.setTimerIfNoneExists(source, isAliveSendTimeout, SENDTIMERCLASS);
    }
  }
  
  /**
   * Informs the manager that a message was sent.
   * 
   * @param destination The destination of the message.
   */
  public void messageSent(Identifier destination) {
    // We sent a regular message so we don't have to send an IsAliveMessage
    timerManager.remove(destination, SENDTIMERCLASS);

    // We expect an answer. Set timer
    timerManager.setTimerIfNoneExists(destination, isAliveTimeout, RECEIVETIMERCLASS);
  }


  public void start() {
    timerManager.start();
  }

  public void stop() {
    timerManager.stop();
  }
  

  @Override
  public void expired(Identifier identifier, int timerClass) throws IOException {
    switch (timerClass) {
      case SENDTIMERCLASS:
        sendExpired(identifier);
        break;
      case RECEIVETIMERCLASS:
        receiveExpired(identifier);
        break;
      default:
        logger.log(Level.WARNING, "Invalid timerClass " + timerClass);
        break;
    }
  }
  
  private void sendExpired(Identifier identifier) {
    // The sent timer expired so we didn't send a regular message since we received the last message
    logger.log(Level.INFO, "Sending IsAliveMessage to " + identifier);
    // Send an IsAliveMessage
    ptp.sendIsAlive(identifier, isAliveSendTimeout);
  }
  
  private void receiveExpired(Identifier identifier) {
    // We didn't get an answer to our last message. Kill the connection.
    logger.log(Level.INFO, "Connection to " + identifier + " timed out.");
    ptp.closeConnections(identifier);
  }
}
