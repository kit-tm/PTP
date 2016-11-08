package edu.kit.tm.ptp.auth;

import edu.kit.tm.ptp.Identifier;
import edu.kit.tm.ptp.channels.ChannelMessageListener;
import edu.kit.tm.ptp.channels.MessageChannel;

/**
 * Abstract class to authenticate connections to hidden services.
 * 
 * @author Timon Hackenjos
 *
 */

public abstract class Authenticator implements ChannelMessageListener {
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
   * Authenticate a connection with an unknown remote end. Waits for the remote end to
   * authenticate itself.
   * 
   * @param own Own identifier.
   */
  public abstract void authenticate(Identifier own);


  /**
   * Authenticate a connection to the supplied identifier.
   * 
   * @param own Own identifier to reveal to the remote end.
   * @param other Expected identifier of the remote end.
   */
  public abstract void authenticate(Identifier own, Identifier other);
}
