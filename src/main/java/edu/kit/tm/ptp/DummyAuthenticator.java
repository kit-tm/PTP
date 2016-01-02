package edu.kit.tm.ptp;

import edu.kit.tm.ptp.channels.ChannelListener;
import edu.kit.tm.ptp.channels.MessageChannel;
import edu.kit.tm.ptp.serialization.Serializer;

import java.io.IOException;

public class DummyAuthenticator extends Authenticator implements ChannelListener {
  private Serializer serializer;
  private ChannelListener oldListener;

  DummyAuthenticator(AuthenticationListener listener, MessageChannel channel,
      Serializer serializer) {
    super(listener, channel);
    this.serializer = serializer;
    //serializer.registerClass(AuthenticationMessage.class);
  }


  @Override
  public void authenticate(Identifier identifier) {
    AuthenticationMessage message = new AuthenticationMessage(identifier);
    byte[] data = serializer.serialize(message);
    oldListener = channel.getChannenListener();
    channel.setChannelListener(this);
    channel.addMessage(data, 0);
  }


  public static class AuthenticationMessage {
    private Identifier source;
    
    public AuthenticationMessage() {
      this.source = null;
    }

    public AuthenticationMessage(Identifier identifier) {
      this.source = identifier;
    }
  }


  @Override
  public void messageSent(long id, MessageChannel destination) {}

  @Override
  public void messageReceived(byte[] data, MessageChannel source) {
    channel.setChannelListener(oldListener);
    
    try {
      Object message = serializer.deserialize(data);

      if (!(message instanceof AuthenticationMessage)) {
        authListener.authenticationFailed(channel);
      } else {
        AuthenticationMessage authMessage = (AuthenticationMessage) message;
        authListener.authenticationSuccess(channel, authMessage.source);
      }
    } catch (IOException e) {
      authListener.authenticationFailed(channel);
      // TODO log error
    }
  }

  @Override
  public void channelOpened(MessageChannel channel) {}

  @Override
  public void channelClosed(MessageChannel channel) {}

}
