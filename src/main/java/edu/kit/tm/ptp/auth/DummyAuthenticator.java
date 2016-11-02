package edu.kit.tm.ptp.auth;

import edu.kit.tm.ptp.Identifier;
import edu.kit.tm.ptp.channels.ChannelMessageListener;
import edu.kit.tm.ptp.channels.MessageChannel;
import edu.kit.tm.ptp.serialization.Serializer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dummy implementation of an Authenticator. Just sends over the own Identifier and trusts the
 * Identifer presented by the remote end.
 * 
 * @author Timon Hackenjos
 *
 */

public class DummyAuthenticator extends Authenticator {
  private boolean sent;
  private boolean received;
  private byte[] response;
  private static final Logger logger = Logger.getLogger(DummyAuthenticator.class.getName());
  private static Serializer serializer = null;

  /**
   * Constructs a new DummyAuthenticator.
   * 
   * @param listener The listener to inform about the authentication.
   * @param channel The channel to authenticate.
   */
  public DummyAuthenticator(AuthenticationListener listener, MessageChannel channel) {
    super(listener, channel);

    sent = false;
    received = false;
    response = null;

    initSerializer();
  }

  private static void initSerializer() {
    if (serializer == null) {
      serializer = new Serializer();
      serializer.registerClass(Identifier.class);
      serializer.registerClass(AuthenticationMessage.class);
    }
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
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "MessageChannel uses"
            + "a new buffer for each message and doesn't change old buffers.")
  public void messageReceived(byte[] data, MessageChannel source) {
    if (!channel.equals(source)) {
      logger.log(Level.WARNING, "Received message from wrong channel");
      return;
    }

    response = data;
    received = true;

    if (sent) {
      finishAuth();
    }
  }

  private void finishAuth() {
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
  public void authenticate(Identifier own) {
    AuthenticationMessage message = new AuthenticationMessage(own);
    byte[] data = serializer.serialize(message);
    channel.addMessage(data, 0);
  }

  @Override
  public void authenticate(Identifier own, Identifier other) {
    authenticate(own);
  }
}
