package edu.kit.tm.ptp;

import edu.kit.tm.ptp.connection.ExpireListener;
import edu.kit.tm.ptp.connection.TimerManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class to manage the sending of keepalive messages and closing expired connections.
 * 
 * @author Timon Hackenjos
 *
 */
public class KeepaliveManager implements ExpireListener {
  private final PTP ptp;
  // Doesn't need synchronization because it's only altered by the connection manager thread
  private Map<Identifier, Boolean> awaitsMessage = new HashMap<Identifier, Boolean>();
  private static final int KEEPALIVETIMEOUT = 30 * 1000;
  private static final int SENDTIME = 10 * 1000;
  
  private static final int SENDTIMERCLASS = 0;
  private static final int RECEIVETIMERCLASS = 1;

  private TimerManager ttlManager;

  private static final Logger logger = Logger.getLogger(KeepaliveManager.class.getName());

  public KeepaliveManager(PTP ptp, Configuration config) {
    this.ptp = ptp;
    ttlManager = new TimerManager(this, config.getTTLPoll());
  }
  
  /**
   * Informs the manager that a message was received.
   * 
   * @param source The identifier of the source of the message.
   * @param isKeepalive True if the message was a keepalive.
   */
  public void messageReceived(Identifier source, boolean isKeepalive) {
    ttlManager.remove(source, RECEIVETIMERCLASS);
    
    if (!isKeepalive) {
      awaitsMessage.put(source, true);
      ttlManager.setTimerIfNoneExists(source, KEEPALIVETIMEOUT - SENDTIME, SENDTIMERCLASS);  
    }
  }
  
  /**
   * Informs the manager that a message was sent.
   * 
   * @param destination The destination of the message.
   */
  public void messageSent(Identifier destination) {
    ttlManager.setTimerIfNoneExists(destination, KEEPALIVETIMEOUT, RECEIVETIMERCLASS);

    awaitsMessage.put(destination, false);
    ttlManager.remove(destination, SENDTIMERCLASS);
  }


  public void start() {
    ttlManager.start();
  }

  public void stop() {
    ttlManager.stop();
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
    Boolean awaits = awaitsMessage.get(identifier);
    if (awaits == null || awaits) {
      logger.log(Level.INFO, "Sending keepalive to " + identifier);
      ptp.sendKeepAlive(identifier, KEEPALIVETIMEOUT - SENDTIME);
      awaits = false;
      
      if (awaits) {
        throw new IllegalStateException();
      }
    }
  }
  
  private void receiveExpired(Identifier identifier) {
    logger.log(Level.INFO, "Connection to " + identifier + " timed out.");
    ptp.closeConnections(identifier);
  }
}
