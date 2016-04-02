package edu.kit.tm.ptp.auth;

import edu.kit.tm.ptp.Identifier;
import edu.kit.tm.ptp.channels.MessageChannel;
import edu.kit.tm.ptp.serialization.Serializer;

/**
 * Abstract class to authenticate connections to hidden services.
 * 
 * @author Timon Hackenjos
 *
 */

public abstract class Authenticator {
  protected AuthenticationListener authListener;
  protected MessageChannel channel;
  protected Serializer serializer;

  /**
   * Constructs a new Authenticator.
   * 
   * @param listener The listener to inform about a successful or failed authentication.
   * @param channel The channel which should be authenticated.
   */
  public Authenticator(AuthenticationListener listener, MessageChannel channel,
      Serializer serializer) {
    this.authListener = listener;
    this.channel = channel;
    this.serializer = serializer;
  }

  /**
   * Tries to authenticate a connection with an unknown remote end. Waits for the other end to
   * authenticate itself first.
   * 
   * @param ownIdentifier Own identifier to reveal to the remote end.
   */
  public abstract void authenticate(Identifier own);


  /**
   * Tries to authenticate a connection to the supplied identifier.
   * 
   * @param own Own identifier to reveal to the remote end.
   * @param other Expected identifier of the remote end.
   */
  public abstract void authenticate(Identifier own, Identifier other);
}
