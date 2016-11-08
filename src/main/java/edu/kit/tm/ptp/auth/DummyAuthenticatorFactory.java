package edu.kit.tm.ptp.auth;

import edu.kit.tm.ptp.channels.MessageChannel;
import edu.kit.tm.ptp.connection.ConnectionManager;

/**
 * Factory class for the DummyAuthenticator.
 * 
 * @author Timon Hackenjos
 *
 */
public class DummyAuthenticatorFactory extends AuthenticatorFactory {

  @Override
  public Authenticator createInstance(ConnectionManager manager, AuthenticationListener listener,
      MessageChannel channel) {
    return new DummyAuthenticator(listener, channel);
  }

}
