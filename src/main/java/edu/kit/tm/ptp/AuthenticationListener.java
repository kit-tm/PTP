package edu.kit.tm.ptp;

import edu.kit.tm.ptp.channels.MessageChannel;

public interface AuthenticationListener {
  public void authenticationSuccess(MessageChannel channel, Identifier identifier);
  public void authenticationFailed(MessageChannel channel);
}
