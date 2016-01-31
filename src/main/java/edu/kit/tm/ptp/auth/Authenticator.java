package edu.kit.tm.ptp.auth;

import edu.kit.tm.ptp.Identifier;
import edu.kit.tm.ptp.channels.MessageChannel;

/**
 * Abstract class to authenticate connections to hidden services.
 * 
 * @author Timon Hackenjos
 *
 */

public abstract class Authenticator {
  protected AuthenticationListener authListener;
  protected MessageChannel channel;
  
  /**
   * Constructs a new Authenticator.
   * 
   * @param listener The listener to inform about a successful or failed authentication.
   * @param channel The channel which should be authenticated.
   */
  public Authenticator(AuthenticationListener listener, MessageChannel channel) {
    this.authListener = listener;
    this.channel = channel;
  }
  
  /**
   * Tries to authenticate the connection.
   * 
   * @param ownIdentifier Own identifier to reveal to the remote end.
   */
  public abstract void authenticate(Identifier ownIdentifier);
}
