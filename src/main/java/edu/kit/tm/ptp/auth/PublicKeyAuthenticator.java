package edu.kit.tm.ptp.auth;

import edu.kit.tm.ptp.Identifier;
import edu.kit.tm.ptp.channels.ChannelMessageListener;
import edu.kit.tm.ptp.channels.MessageChannel;
import edu.kit.tm.ptp.crypt.CryptHelper;
import edu.kit.tm.ptp.serialization.Serializer;
import edu.kit.tm.ptp.utility.Constants;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Authenticator using an RSA signature.
 * 
 * @author Timon Hackenjos
 *
 */
// TODO restrict usable public keys and algorithms?
public class PublicKeyAuthenticator extends Authenticator implements ChannelMessageListener {
  private static final Logger logger = Logger.getLogger(PublicKeyAuthenticator.class.getName());
  private ChannelMessageListener oldListener;
  private boolean sendSuccess;
  private boolean sent;
  private boolean received;
  private static final Map<Identifier, HashSet<Long>> nonces =
      new HashMap<Identifier, HashSet<Long>>();
  protected Identifier own = null;
  protected Identifier other = null;
  private static final long authenticatorLifetime = 60 * 1000; // in ms
  private static final long maxClockDifference = 20 * 1000; // in ms
  private CryptHelper cryptHelper;

  public PublicKeyAuthenticator(AuthenticationListener listener, MessageChannel channel,
      Serializer serializer, CryptHelper cryptHelper) {
    super(listener, channel, serializer);
    this.cryptHelper = cryptHelper;
  }

  /**
   * Message to authenticate oneself against another PTP instance.
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
    public long nonce;
    /** An RSA signature using private key of the hidden service. */
    public byte[] signature;

    public AuthenticationMessage() {
      source = null;
      destination = null;
      pubKey = null;
      nonce = -1;
      signature = null;
    }

    public AuthenticationMessage(Identifier source, Identifier destination, byte[] pubKey,
        long nonce, byte[] signature) {
      this.source = source;
      this.destination = destination;
      this.pubKey = pubKey;
      this.nonce = nonce;
      this.signature = signature;
    }
  }

  @Override
  public void messageSent(long id, MessageChannel destination) {
    if (!channel.equals(destination)) {
      logger.log(Level.SEVERE, "Wrong message channel.");
      authFailed();
      return;
    }

    if (id != 0) {
      logger.log(Level.WARNING, "Unexpected message with id " + id + " has been sent");
      authFailed();
      return;
    }

    sendSuccess = true;

    if (received) {
      authSuccess();
    }
  }

  @Override
  public void messageReceived(byte[] data, MessageChannel source) {
    if (!channel.equals(source)) {
      logger.log(Level.WARNING, "Received message from wrong channel");
      authFailed();
      return;
    }

    received = true;

    AuthenticationMessage authMessage;
    try {
      authMessage = deserialize(data);
    } catch (IOException e) {
      logger.log(Level.INFO, "Unable to deserialize received authentication message");
      authFailed();
      return;
    }

    if (authMessage == null || !authenticatorValid(authMessage)) {
      authFailed();
      return;
    }

    if (other == null) {
      other = authMessage.source;
    }

    if (sendSuccess) {
      authSuccess();
    } else if (!sent) {
      AuthenticationMessage response;

      try {
        response = createAuthenticator();
      } catch (GeneralSecurityException | UnsupportedEncodingException e) {
        logger.log(Level.WARNING, "Failed to create authentication message");
        authFailed();
        return;
      }

      sendAuthMessage(response);
      sent = true;
    }
  }

  @Override
  public void authenticate(Identifier own) {
    if (own == null) {
      throw new IllegalArgumentException();
    }

    this.own = own;

    oldListener = channel.getChannelMessageListener();
    channel.setChannelMessageListener(this);
  }

  @Override
  public void authenticate(Identifier own, Identifier other) {
    if (other == null || own == null) {
      throw new IllegalArgumentException();
    }

    this.own = own;
    this.other = other;

    try {
      AuthenticationMessage auth = createAuthenticator();

      oldListener = channel.getChannelMessageListener();
      channel.setChannelMessageListener(this);

      sendAuthMessage(auth);
      sent = true;
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

  protected boolean authenticatorValid(AuthenticationMessage message) {
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

    if (containsNonce(message.source, message.nonce)) {
      logger.log(Level.WARNING, "Received same authentication message again");
      return false;
    }

    if (isNonceExpired(message.nonce)) {
      logger.log(Level.WARNING, "Received expired authentication message");
      return false;
    }

    if (message.nonce - System.currentTimeMillis() > maxClockDifference) {
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

      if (cryptHelper.verifySignature(getBytes(message), message.signature, pubKey)) {
        addNonce(message.source, message.nonce);
        return true;
      }

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

  protected AuthenticationMessage createAuthenticator()
      throws GeneralSecurityException, UnsupportedEncodingException {
    return createAuthenticator(System.currentTimeMillis());
  }

  protected AuthenticationMessage createAuthenticator(long nonce)
      throws GeneralSecurityException, UnsupportedEncodingException {
    byte[] pubKey = cryptHelper.getPublicKeyBytes();
    ByteBuffer toSign = getBytes(own, other, pubKey, nonce);

    byte[] signature = cryptHelper.sign(toSign);

    AuthenticationMessage auth = new AuthenticationMessage(own, other, pubKey, nonce, signature);

    return auth;
  }

  private ByteBuffer getBytes(Identifier source, Identifier destination, byte[] pubKey,
      long nonce) throws UnsupportedEncodingException {
    byte[] sourceBytes = source.toString().getBytes(Constants.charset);
    byte[] destinationBytes = source.toString().getBytes(Constants.charset);

    int length = sourceBytes.length + destinationBytes.length + pubKey.length + 8;

    ByteBuffer buffer = ByteBuffer.allocate(length);
    buffer.put(sourceBytes);
    buffer.put(destinationBytes);
    buffer.put(pubKey);
    buffer.putLong(nonce);

    buffer.rewind();
    return buffer;
  }

  private ByteBuffer getBytes(AuthenticationMessage message) throws UnsupportedEncodingException {
    return getBytes(message.source, message.destination, message.pubKey, message.nonce);
  }

  private boolean containsNonce(Identifier other, Long nonce) {
    synchronized (nonces) {
      HashSet<Long> userNonces = nonces.get(other);

      if (userNonces == null) {
        return false;
      }

      return userNonces.contains(nonce);
    }
  }

  private void addNonce(Identifier other, Long nonce) {
    synchronized (nonces) {
      logger.log(Level.INFO, "Adding nonce " + nonce + " for " + other.toString());
      HashSet<Long> userNonces = nonces.get(other);

      if (userNonces == null) {
        userNonces = new HashSet<Long>();
        nonces.put(other, userNonces);
      }

      removeOldNonces(userNonces);

      userNonces.add(nonce);
    }
  }

  private boolean isNonceExpired(long nonce) {
    return nonce + authenticatorLifetime < System.currentTimeMillis();
  }
  
  private void removeOldNonces(HashSet<Long> userNonces) {
    Iterator<Long> nonceIterator = userNonces.iterator();

    while (nonceIterator.hasNext()) {
      if (isNonceExpired(nonceIterator.next())) {
        nonceIterator.remove();
      }
    }
  }
}
