package edu.kit.tm.ptp;

import edu.kit.tm.ptp.channels.ChannelListener;
import edu.kit.tm.ptp.channels.MessageChannel;
import edu.kit.tm.ptp.serialization.Serializer;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dummy implementation of an Authenticator.
 * Just sends over the own Identifier and trusts the
 * Identifer presented by the remote end.
 * 
 * @author Timon Hackenjos
 *
 */

public class DummyAuthenticator extends Authenticator implements ChannelListener {
  private Serializer serializer;
  private ChannelListener oldListener;
  private boolean sent;
  private boolean received;
  private byte[] response;
  private static final Logger logger = Logger.getLogger(DummyAuthenticator.class.getName());

  /**
   * Constructs a new DummyAuthenticator.
   * 
   * @param listener The listener to inform about the authentication.
   * @param channel The channel to authenticate.
   * @param serializer To serialize the authentication messages.
   */
  public DummyAuthenticator(AuthenticationListener listener, MessageChannel channel,
      Serializer serializer) {
    super(listener, channel);
    this.serializer = serializer;

    sent = false;
    received = false;
    response = null;
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
  public void messageSent(long id, MessageChannel destination) {
    if (id != 0) {
      logger.log(Level.WARNING, "Unexpected message with id " + id + " has been sent");
      return;
    }
    
    sent = true;
    
    if (received) {
      finishAuth();
    }
  }

  @Override
  public void messageReceived(byte[] data, MessageChannel source) {
    if (!channel.equals(source)) {
      logger.log(Level.WARNING, "Received message from wrong channel");
      return;
    }
    
    received = true;
    response = data;
    
    if (sent) {
      finishAuth();
    }
  }
  
  private void finishAuth() {
    channel.setChannelListener(oldListener);
    
    try {
      Object message = serializer.deserialize(response);

      if (!(message instanceof AuthenticationMessage)) {
        authListener.authenticationFailed(channel);
      } else {
        AuthenticationMessage authMessage = (AuthenticationMessage) message;
        authListener.authenticationSuccess(channel, authMessage.source);
      }
    } catch (IOException e) {
      logger.log(Level.INFO, "Authentication failed: " + e.getMessage());
      authListener.authenticationFailed(channel);
    }
  }

  @Override
  public void channelOpened(MessageChannel channel) {}

  @Override
  public void channelClosed(MessageChannel channel) {}

}
