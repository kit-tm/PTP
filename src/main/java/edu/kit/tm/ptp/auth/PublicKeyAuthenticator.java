package edu.kit.tm.ptp.auth;

import edu.kit.tm.ptp.Identifier;
import edu.kit.tm.ptp.channels.ChannelMessageListener;
import edu.kit.tm.ptp.channels.MessageChannel;
import edu.kit.tm.ptp.crypt.CryptHelper;
import edu.kit.tm.ptp.serialization.Serializer;
import edu.kit.tm.ptp.utility.Constants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Authenticator using an RSA signature. The initiator of the authentication sends an
 * AuthenticationMessage to the target. The target checks the validity of the AuthenticationMessage
 * and responds with a AUTHENTICATION_SUCCESS_MESSAGE in that case. Otherwise the channel will be
 * closed.
 * 
 * @author Timon Hackenjos
 *
 */
public class PublicKeyAuthenticator extends Authenticator {
  protected Identifier own = null;
  protected Identifier other = null;

  private static final Logger logger = Logger.getLogger(PublicKeyAuthenticator.class.getName());
  private static final byte AUTHENTICATION_SUCCESS_MESSAGE = 0x0;
  private static final long TIMESTAMP_INTERVALL = 60 * 1000; // in ms

  private ChannelMessageListener oldListener;
  private final CryptHelper cryptHelper;
  private final Serializer serializer;

  public PublicKeyAuthenticator(AuthenticationListener listener, MessageChannel channel,
      CryptHelper cryptHelper) {
    super(listener, channel);
    this.cryptHelper = cryptHelper;

    this.serializer = new Serializer();
    serializer.registerClass(AuthenticationMessage.class);
  }

  /**
   * Message to authenticate oneself against another PTP instance. The message contains a timestamp
   * and is only valid for a limited amount of time defined by TIMESTAMP_INTERVALL.
   * 
   * @author Timon Hackenjos
   *
   */
  public static class AuthenticationMessage {
    /** The hidden service identifier of the sender. */
    public Identifier source;
    /** The hidden service identifier of the receiver. */
    public Identifier destination;
    /** The public key of the sender. */
    public byte[] pubKey;
    /**
     * The time the message was generated.
     * 
     * @see System#currentTimeMillis()
     */
    public long timestamp;
    /** An RSA signature using private key of the hidden service. */
    public byte[] signature;

    public AuthenticationMessage() {
      source = null;
      destination = null;
      pubKey = null;
      timestamp = -1;
      signature = null;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "AuthenticationMessage is just a simple container."
            + " Avoid to copy data several times.")
    public AuthenticationMessage(Identifier source, Identifier destination, byte[] pubKey,
        long timestamp, byte[] signature) {
      this.source = source;
      this.destination = destination;
      this.pubKey = pubKey;
      this.timestamp = timestamp;
      this.signature = signature;
    }
  }

  /**
   * Contains the behavior if we are the initiator of the authentication.
   */
  public class InitiatorListener implements ChannelMessageListener {
    @Override
    public void messageSent(long id, MessageChannel destination) {
      assert id == 0;
      assert channel.equals(destination);
    }

    @Override
    public void messageReceived(byte[] data, MessageChannel source) {
      assert channel.equals(source);

      if (data.length == 1 && data[0] == AUTHENTICATION_SUCCESS_MESSAGE) {
        authSuccess();
      } else {
        authFailed();
      }
    }
  }

  /**
   * Contains the behavior if we are the target of the authentication.
   */
  public class TargetListener implements ChannelMessageListener {
    @Override
    public void messageSent(long id, MessageChannel destination) {
      assert id == 0;
      assert channel.equals(destination);

      // Authentication success message has been sent successfully
      authSuccess();
    }

    @Override
    public void messageReceived(byte[] data, MessageChannel source) {
      assert channel.equals(source);

      AuthenticationMessage authMessage;

      // deserialize received message
      try {
        authMessage = deserialize(data);
      } catch (IOException e) {
        logger.log(Level.INFO, "Unable to deserialize received authentication message");
        authFailed();
        return;
      }

      // check if the authentication message is valid
      if (authMessage == null || !authenticationMessageValid(authMessage)) {
        authFailed();
        return;
      }

      // now we know the identifier of the initiator
      other = authMessage.source;

      // respond with message to signal successfull authentication
      channel.addMessage(new byte[] {AUTHENTICATION_SUCCESS_MESSAGE}, 0);
    }
  }

  @Override
  public void authenticate(Identifier own) {
    if (own == null) {
      throw new IllegalArgumentException();
    }

    this.own = own;

    oldListener = channel.getChannelMessageListener();

    // Wait for the other end to send an authentication message
    channel.setChannelMessageListener(new TargetListener());
  }

  @Override
  public void authenticate(Identifier own, Identifier other) {
    if (other == null || own == null) {
      throw new IllegalArgumentException();
    }

    this.own = own;
    this.other = other;

    oldListener = channel.getChannelMessageListener();
    channel.setChannelMessageListener(new InitiatorListener());

    // We initiated the authentication process and therefore send an authentication message
    try {
      sendAuthMessage(createAuthenticationMessage());
    } catch (GeneralSecurityException | UnsupportedEncodingException e) {
      logger.log(Level.WARNING, "Failed to sign authentication message");
      authFailed();
    }
  }

  private void authFailed() {
    channel.setChannelMessageListener(oldListener);
    authListener.authenticationFailed(channel);
  }

  private void authSuccess() {
    channel.setChannelMessageListener(oldListener);
    authListener.authenticationSuccess(channel, other);
  }

  private void sendAuthMessage(AuthenticationMessage message) {
    byte[] data = serializer.serialize(message);
    channel.addMessage(data, 0);
  }

  private AuthenticationMessage deserialize(byte[] data) throws IOException {
    Object message = serializer.deserialize(data);

    if (!(message instanceof AuthenticationMessage)) {
      logger.log(Level.INFO, "Received invalid message");
      return null;
    } else {
      return (AuthenticationMessage) message;
    }
  }

  /**
   * Checks if the authentication message is valid which means that the destination identifier
   * equals our own identifier, the timestamp isn't expired or invalid, the public key and the
   * source identifier match, the signature is valid.
   *
   * @param message The message to check.
   * @return True if the message is valid.
   */
  protected boolean authenticationMessageValid(AuthenticationMessage message) {
    if (message.source == null || message.destination == null || message.pubKey == null
        || message.signature == null) {
      logger.log(Level.WARNING, "Authentication message contains null values");
      return false;
    }

    if (!own.equals(message.destination)) {
      logger.log(Level.WARNING, "Received authentication message with wrong destination");
      return false;
    }

    if (other != null && !other.equals(message.source)) {
      logger.log(Level.WARNING, "Remote end authenticated itself as different user as expected");
      return false;
    }

    long currentTime = System.currentTimeMillis();

    if (currentTime - message.timestamp > TIMESTAMP_INTERVALL) {
      logger.log(Level.WARNING, "Received expired authentication message");
      return false;
    }

    if (message.timestamp - currentTime > TIMESTAMP_INTERVALL) {
      logger.log(Level.WARNING,
          "Received authentication message which expires too far in the future.");
      return false;
    }

    if (!message.source.isValid()) {
      logger.log(Level.WARNING, "Authentication message contains invalid source identifier");
      return false;
    }

    try {
      PublicKey pubKey = cryptHelper.decodePublicKey(message.pubKey);
      Identifier pubKeyIdentifier = cryptHelper.calculateHiddenServiceIdentifier(pubKey);

      if (pubKeyIdentifier == null || !message.source.equals(pubKeyIdentifier)) {
        logger.log(Level.WARNING,
            "Identifier and public key of authentication message do not match");
        return false;
      }

      return cryptHelper.verifySignature(getBytes(message), message.signature, pubKey);

    } catch (InvalidKeySpecException e) {
      logger.log(Level.WARNING, "Failed to read public key in authentication message");
    } catch (InvalidKeyException e) {
      logger.log(Level.WARNING, "Invalid public key in authentication message");
    } catch (SignatureException e) {
      logger.log(Level.WARNING, "Failed to verify signature");
    } catch (IOException e) {
      logger.log(Level.WARNING, "Failed to calculate hidden service identifier of public key");
    }

    return false;
  }

  protected AuthenticationMessage createAuthenticationMessage()
      throws GeneralSecurityException, UnsupportedEncodingException {
    return createAuthenticationMessage(System.currentTimeMillis());
  }

  protected AuthenticationMessage createAuthenticationMessage(long timestamp)
      throws GeneralSecurityException, UnsupportedEncodingException {
    byte[] pubKey = cryptHelper.getPublicKeyBytes();
    ByteBuffer toSign = getBytes(own, other, pubKey, timestamp);

    byte[] signature = cryptHelper.sign(toSign);

    AuthenticationMessage auth =
        new AuthenticationMessage(own, other, pubKey, timestamp, signature);

    return auth;
  }

  private ByteBuffer getBytes(Identifier source, Identifier destination, byte[] pubKey,
      long timestamp) throws UnsupportedEncodingException {
    byte[] sourceBytes = source.toString().getBytes(Constants.charset);
    byte[] destinationBytes = source.toString().getBytes(Constants.charset);

    int length = sourceBytes.length + destinationBytes.length + pubKey.length + 8;

    ByteBuffer buffer = ByteBuffer.allocate(length);
    buffer.put(sourceBytes);
    buffer.put(destinationBytes);
    buffer.put(pubKey);
    buffer.putLong(timestamp);

    buffer.rewind();
    return buffer;
  }

  private ByteBuffer getBytes(AuthenticationMessage message) throws UnsupportedEncodingException {
    return getBytes(message.source, message.destination, message.pubKey, message.timestamp);
  }
}
