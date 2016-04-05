package edu.kit.tm.ptp.crypt;

import edu.kit.tm.ptp.Identifier;

import org.apache.commons.codec.binary.Base32;

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

/**
 * Helper class for cryptographic operations.
 * 
 * @author Timon Hackenjos
 */

public class CryptHelper {
  protected KeyPair keyPair = null;
  private Signature sign = null;
  private Signature verify = null;
  private MessageDigest sha1 = null;
  private KeyFactory rsaFactory = null;
  private Base32 base32 = new Base32();

  public CryptHelper() {
    Security.addProvider(new BouncyCastleProvider());
  }

  /**
   * Initializes the helper class. Has to be the first method to be called.
   * 
   * @throws NoSuchAlgorithmException If a used algorithm isn't available.
   * @throws NoSuchProviderException If the used provider isn't available.
   */
  public void init() throws NoSuchAlgorithmException, NoSuchProviderException {
    // TODO use SHA1 like Tor?
    sign = Signature.getInstance("SHA256withRSA");
    verify = Signature.getInstance("SHA256withRSA");
    sha1 = MessageDigest.getInstance("SHA1");
    rsaFactory = KeyFactory.getInstance("RSA");
  }

  /**
   * Sets the KeyPair to use for signing.
   * 
   * @throws InvalidKeyException If the KeyPair can't be used.
   */
  public void setKeyPair(KeyPair keyPair) throws InvalidKeyException {
    if (sign == null) {
      throw new IllegalStateException("Call init first");
    }

    this.keyPair = keyPair;
    sign.initSign(keyPair.getPrivate());
  }

  /**
   * Returns the encoded public key of the currently used keypair.
   */
  public byte[] getPublicKeyBytes() {
    return keyPair.getPublic().getEncoded();
  }

  /**
   * Signs the supplied data with the private key of the current keypair.
   * 
   * @param data The bytes to sign.
   * @return The signature.
   * @throws SignatureException If an error occurs while signing.
   */
  public byte[] sign(ByteBuffer data) throws SignatureException {
    if (keyPair == null) {
      throw new IllegalStateException("PrivateKey hasn't been set.");
    }

    if (sign == null) {
      throw new IllegalStateException("Call init first");
    }

    sign.update(data);

    return sign.sign();
  }

  /**
   * Verifies a signature.
   * 
   * @param data The bytes that have been signed.
   * @param signature The signature.
   * @param pubKey The public key of the signer.
   * @return True if the signature is valid.
   * 
   * @throws InvalidKeyException If the public key can't be used.
   * @throws SignatureException If an error occurs while verifying the signature.
   */
  public boolean verifySignature(ByteBuffer data, byte[] signature, PublicKey pubKey)
      throws InvalidKeyException, SignatureException {
    if (verify == null) {
      throw new IllegalStateException("Call init first");
    }

    verify.initVerify(pubKey);
    verify.update(data);

    return verify.verify(signature);
  }

  /**
   * Reads a keypair from a File.
   * 
   * @throws IOException If an error occurs while reading the file.
   * @throws InvalidKeySpecException If the key isn't encoded in x509.
   */
  public KeyPair readKeyPairFromFile(File file) throws IOException, InvalidKeySpecException {
    PEMParser parser = new PEMParser(new FileReader(file));

    Object obj = parser.readObject();
    parser.close();

    if (obj instanceof PEMKeyPair) {
      PEMKeyPair pem = (PEMKeyPair) obj;
      JcaPEMKeyConverter conv = new JcaPEMKeyConverter();

      return conv.getKeyPair(pem);
    }

    return null;
  }

  /**
   * Decodes an encoded public key.
   * 
   * @param pubKeyBytes The bytes of the public key.
   * @return The public key.
   * 
   * @throws InvalidKeySpecException If the public key isn't encoded in x509.
   */
  public PublicKey decodePublicKey(byte[] pubKeyBytes) throws InvalidKeySpecException {
    if (rsaFactory == null) {
      throw new IllegalStateException("Call init first");
    }

    X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(pubKeyBytes);
    return rsaFactory.generatePublic(pubKeySpec);
  }

  /**
   * Calculates a hidden service identifier from a public key.
   * 
   * @throws IOException If encoding the public key fails.
   */
  public Identifier calculateHiddenServiceIdentifier(PublicKey pubKey) throws IOException {
    if (sha1 == null) {
      throw new IllegalStateException("Call init first");
    }

    SubjectPublicKeyInfo spkInfo = SubjectPublicKeyInfo.getInstance(pubKey.getEncoded());
    /*
     * An RSA public key shall have ASN.1 type RSAPublicKey:
     * 
     * RSAPublicKey ::= SEQUENCE { modulus INTEGER, -- n publicExponent INTEGER -- e }
     */
    byte[] bytes = spkInfo.parsePublicKey().getEncoded("DER");

    // H(PK)
    byte[] hash = sha1.digest(bytes);

    // first 80 bits of H(PK)
    byte[] firstBytes = new byte[10];

    for (int i = 0; i < firstBytes.length; i++) {
      firstBytes[i] = hash[i];
    }

    // base32 encoding
    String identifier = new String(base32.encode(firstBytes)) + ".onion";

    return new Identifier(identifier.toLowerCase());
  }
}
