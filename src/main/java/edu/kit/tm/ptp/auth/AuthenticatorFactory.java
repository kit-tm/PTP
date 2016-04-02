package edu.kit.tm.ptp.auth;

import edu.kit.tm.ptp.channels.MessageChannel;
import edu.kit.tm.ptp.connection.ConnectionManager;
import edu.kit.tm.ptp.serialization.Serializer;

/**
 * Factory class for Authenticators.
 * 
 * @author Timon Hackenjos
 *
 */
public abstract class AuthenticatorFactory {
  /**
   * Creates a new Authenticator object.
   * 
   * @param manager The ConnectionManager.
   * @param listener The listener to inform about successful and failed authentication attempts.
   * @param channel The channel to authenticate.
   * @param serializer Serializer to allow serialization of authentication messages.
   * @return The created object.
   */
  public abstract Authenticator createInstance(ConnectionManager manager,
      AuthenticationListener listener, MessageChannel channel, Serializer serializer);
}
