package edu.kit.tm.ptp.auth;

import edu.kit.tm.ptp.channels.MessageChannel;
import edu.kit.tm.ptp.connection.ConnectionManager;
import edu.kit.tm.ptp.serialization.Serializer;

/**
 * Factory class for the PublicKeyAuthenticator.
 * 
 * @author Timon Hackenjos
 *
 */
public class PublicKeyAuthenticatorFactory extends AuthenticatorFactory {

  @Override
  public Authenticator createInstance(ConnectionManager manager, AuthenticationListener listener,
      MessageChannel channel, Serializer serializer) {
    return new PublicKeyAuthenticator(listener, channel, serializer, manager.getCryptHelper());
  }
}
