package edu.kit.tm.ptp;

import edu.kit.tm.ptp.channels.MessageChannel;

abstract class Authenticator {
  protected AuthenticationListener authListener;
  protected MessageChannel channel;
  
  public Authenticator(AuthenticationListener listener, MessageChannel channel) {
    this.authListener = listener;
    this.channel = channel;
  }
  
  public abstract void authenticate(Identifier identifier);
}
