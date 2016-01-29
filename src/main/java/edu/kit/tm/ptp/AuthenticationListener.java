package edu.kit.tm.ptp;

import edu.kit.tm.ptp.channels.MessageChannel;

/**
 * Listener for authentication attempts.
 * 
 * @author Timon Hackenjos
 *
 */
public interface AuthenticationListener {
  /**
   * Gets called when the authentication was successful.
   * 
   * @param channel The channel which has been authenticated.
   * @param identifier The identifier of the remote end.
   */
  public void authenticationSuccess(MessageChannel channel, Identifier identifier);

  /**
   * Gets called when the authentications failed.
   * 
   * @param channel The channel which should have been authenticated.
   */
  public void authenticationFailed(MessageChannel channel);
}
