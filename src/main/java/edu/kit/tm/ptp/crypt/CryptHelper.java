package edu.kit.tm.ptp.crypt;

import edu.kit.tm.ptp.Identifier;
import edu.kit.tm.ptp.utility.Constants;

import org.apache.commons.codec.binary.Base32;

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jcajce.provider.digest.SHA3;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import edu.kit.tm.ptp.crypt.bouncycastle.Ed25519PrivateKeyParametersMod;
import edu.kit.tm.ptp.crypt.bouncycastle.Ed25519SignerMod;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
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
import java.util.Arrays;

/**
 * Helper class for cryptographic operations.
 * 
 * @author Timon Hackenjos
 */

public class CryptHelper {
  protected Ed25519PublicKeyParameters publicKey = null;
  protected Ed25519PrivateKeyParametersMod privateKey = null;
  private Base32 base32 = new Base32();
  private Ed25519SignerMod sign = null;
  private Ed25519SignerMod verify = null;

  private byte[] publicKeyHeader;
  private byte[] privateKeyHeader;
  public CryptHelper() {
    Security.addProvider(new BouncyCastleProvider());

    try {
      publicKeyHeader = "== ed25519v1-public: type0 ==\000\000\000".getBytes("ASCII");
      privateKeyHeader = "== ed25519v1-secret: type0 ==\000\000\000".getBytes("ASCII");
    } catch (UnsupportedEncodingException e) {
      publicKeyHeader = "== ed25519v1-public: type0 ==\000\000\000".getBytes();
      privateKeyHeader = "== ed25519v1-secret: type0 ==\000\000\000".getBytes();
    }
  }

  /**
   * Initializes the helper class. Has to be the first method to be called.
   * 
   * @throws NoSuchAlgorithmException If a used algorithm isn't available.
   * @throws NoSuchProviderException If the used provider isn't available.
   */
  public void init() throws NoSuchAlgorithmException, NoSuchProviderException {
    sign = new Ed25519SignerMod();
    verify = new Ed25519SignerMod();
  }

  /**
   * Returns the encoded public key of the currently used keypair.
   */
  public byte[] getPublicKeyBytes() {
    if (publicKey == null) {
      throw new IllegalStateException("PublicKey hasn't been set.");
    }
    return publicKey.getEncoded();
  }

  /**
   * Signs the supplied data with the private key of the current keypair.
   * 
   * @param data The bytes to sign.
   * @return The signature.
   * @throws SignatureException If an error occurs while signing.
   */
  public byte[] sign(ByteBuffer data) throws SignatureException {
    if (privateKey == null) {
      throw new IllegalStateException("PrivateKey hasn't been set.");
    }

    if (sign == null) {
      throw new IllegalStateException("Call init first");
    }

    if (data.position() != 0) {
      throw new IllegalStateException("Received ByteBuffer is expected to be rewinded");
    }

    byte[] arr = new byte[data.remaining()];
    data.get(arr);
    sign.update(arr, 0, arr.length);

    return sign.generateSignature();
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
  public boolean verifySignature(ByteBuffer data, byte[] signature, Ed25519PublicKeyParameters pubKey)
      throws InvalidKeyException, SignatureException {
    if (verify == null) {
      throw new IllegalStateException("Call init first");
    }

    if (data.position() != 0) {
      throw new IllegalStateException("Received ByteBuffer is expected to be rewinded");
    }


    verify.init(false, pubKey);

    byte[] arr = new byte[data.remaining()];
    data.get(arr);
    verify.update(arr, 0, arr.length);

    return verify.verifySignature(signature);
  }

  /*
   * Reads the private key used for signing from a file.
   * 
   * @throws InvalidKeyException If the KeyPair can't be used.
   * @throws IOException If an error occurs while reading the file.
   * @throws InvalidKeySpecException If the key isn't encoded in x509.
   */
  public void readKeysFromFiles(File publicKeyFile, File privateKeyFile)  throws IOException, InvalidKeySpecException, InvalidKeyException {

    if (sign == null) {
      throw new IllegalStateException("Call init first");
    }


    // Read the public key
    byte[] fileContent = Files.readAllBytes(publicKeyFile.toPath());
    byte[] header = Arrays.copyOf(fileContent, 32);
    if (fileContent.length != 64 || !Arrays.equals(publicKeyHeader, header)) {
      throw new InvalidKeyException("public key file has wrong format");
    }
    byte[] keyBytes = Arrays.copyOfRange(fileContent, 32, fileContent.length);
    publicKey = new Ed25519PublicKeyParameters(keyBytes, 0);
    // verify.init() is called later on

    // Same for private key
    fileContent = Files.readAllBytes(privateKeyFile.toPath());
    header = Arrays.copyOf(fileContent, 32);
    if (fileContent.length != 96 || !Arrays.equals(privateKeyHeader, header)) {
      throw new InvalidKeyException("private key file has wrong format");
    }
    keyBytes = Arrays.copyOfRange(fileContent, 32, fileContent.length);
    privateKey = new Ed25519PrivateKeyParametersMod(keyBytes, 0);
    sign.init(true, privateKey);

  }

  /**
   * Decodes an encoded public key.
   * 
   * @param pubKeyBytes The bytes of the public key.
   * @return The public key.
   * 
   * @throws InvalidKeySpecException If the public key isn't encoded in x509.
   */
  public Ed25519PublicKeyParameters decodePublicKey(byte[] pubKeyBytes) throws InvalidKeySpecException {
    return new Ed25519PublicKeyParameters(pubKeyBytes, 0);
  }

  /**
   * Calculates a hidden service identifier from a public key.
   * 
   * @throws IOException If encoding the public key fails.
   */
  public Identifier calculateHiddenServiceIdentifier(Ed25519PublicKeyParameters pubKey)
      throws IOException {

    // Based on https://www.reddit.com/r/TOR/comments/bn4q72/explanations_of_v3_address/
    // and https://gitweb.torproject.org/torspec.git/tree/rend-spec-v3.txt#n2160

    ByteBuffer buffer = ByteBuffer.allocate(15 + 32 + 1);

    // CHECKSUM = H(".onion checksum" | PUBKEY | VERSION)[:2]
    buffer.put(".onion checksum".getBytes("ASCII"));
    buffer.put((byte)'\3');
    buffer.rewind();
    byte[] arr = new byte[buffer.remaining()];
    buffer.get(arr);
    SHA3.DigestSHA3 digestSHA3 = new SHA3.Digest256();
    byte[] hash = digestSHA3.digest(arr);

    // onion_address = base32(PUBKEY | CHECKSUM | VERSION)
    buffer = ByteBuffer.allocate(32 + 2 + 1);
    // full public key
    buffer.put(pubKey.getEncoded());
    // checksum is the first 2 bytes of the hash calculated above
    buffer.put(Arrays.copyOf(hash, 2));
    // version number
    buffer.put((byte)'\3');

    buffer.rewind();
    arr = new byte[buffer.remaining()];
    buffer.get(arr);

    // base32 encoding + ".onion"
    String identifier = new String(base32.encode(arr), Constants.charset) + ".onion";

    return new Identifier(identifier.toLowerCase());
  }
}
