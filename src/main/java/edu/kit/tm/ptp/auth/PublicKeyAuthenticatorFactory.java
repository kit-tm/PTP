package edu.kit.tm.ptp.auth;

import edu.kit.tm.ptp.channels.MessageChannel;
import edu.kit.tm.ptp.connection.ConnectionManager;

/**
 * Factory class for the PublicKeyAuthenticator.
 * 
 * @author Timon Hackenjos
 *
 */
public class PublicKeyAuthenticatorFactory extends AuthenticatorFactory {

  @Override
  public Authenticator createInstance(ConnectionManager manager, AuthenticationListener listener,
      MessageChannel channel) {
    return new PublicKeyAuthenticator(listener, channel, manager.getCryptHelper());
  }
}
