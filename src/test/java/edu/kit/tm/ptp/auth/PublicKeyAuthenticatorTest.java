package edu.kit.tm.ptp.auth;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import edu.kit.tm.ptp.Identifier;
import edu.kit.tm.ptp.PTP;
import edu.kit.tm.ptp.auth.PublicKeyAuthenticator.AuthenticationMessage;
import edu.kit.tm.ptp.crypt.CryptHelper;
import edu.kit.tm.ptp.utility.Constants;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.Cipher;


public class PublicKeyAuthenticatorTest {
  private PublicKeyAuthenticator auth;
  private PublicKeyAuthenticator auth2;
  private static CryptHelper cryptHelper;
  private static CryptHelper cryptHelper2;
  private static PTP ptp1;
  private static PTP ptp2;
  private static File pk1;
  private static File pk2;

  @BeforeClass
  public static void setUpClass() throws IOException, InvalidKeyException, InvalidKeySpecException,
      NoSuchAlgorithmException, NoSuchProviderException {
    ptp1 = new PTP(true);
    ptp2 = new PTP(true);

    ptp1.init();
    ptp2.init();

    ptp1.reuseHiddenService();
    ptp2.reuseHiddenService();

    String hsDir1 = ptp1.getHiddenServiceDirectory();
    String hsDir2 = ptp2.getHiddenServiceDirectory();

    pk1 = new File(hsDir1 + File.separator + Constants.prkey);
    pk2 = new File(hsDir2 + File.separator + Constants.prkey);

    cryptHelper = new CryptHelper();
    cryptHelper.init();
    cryptHelper.setKeyPair(CryptHelper.readKeyPairFromFile(pk1));

    cryptHelper2 = new CryptHelper();
    cryptHelper2.init();
    cryptHelper2.setKeyPair(CryptHelper.readKeyPairFromFile(pk2));
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
    ptp1.exit();
    ptp2.exit();
  }

  @Before
  public void setUp() throws Exception {
    auth = new PublicKeyAuthenticator(null, null, cryptHelper);
    auth2 = new PublicKeyAuthenticator(null, null, cryptHelper2);
  }

  @After
  public void tearDown() throws Exception {}

  @Test
  public void testAuthenticator() throws IOException, GeneralSecurityException {
    testAuthenticator(false);
  }
  
  private void testAuthenticator(boolean unknown) throws IOException, GeneralSecurityException {
    auth.own = ptp1.getIdentifier();
    auth.other = ptp2.getIdentifier();
    AuthenticationMessage authMessage = auth.createAuthenticationMessage();

    auth2.own = ptp2.getIdentifier();
    auth2.other = unknown ? null : ptp1.getIdentifier();

    assertEquals(true, auth2.authenticationMessageValid(authMessage));
  }

  @Test
  public void testAuthenticatorUnknown() throws IOException, GeneralSecurityException {
    testAuthenticator(true);
  }

  @Test
  public void testWrongAuthenticator() throws IOException, GeneralSecurityException {
    auth.own = ptp1.getIdentifier();
    auth.other = ptp2.getIdentifier();
    AuthenticationMessage authMessage = auth.createAuthenticationMessage();
    byte[] signature = authMessage.signature;

    // flip bits of first byte
    signature[0] = (byte) (signature[0] ^ 0xff);

    auth2.own = ptp2.getIdentifier();

    assertEquals(false, auth2.authenticationMessageValid(authMessage));
  }

  @Test
  public void testWrongSource() throws IOException, GeneralSecurityException {
    auth.own = new Identifier("aaaaaaaaaaaaaaaa.onion");
    auth.other = ptp2.getIdentifier();
    AuthenticationMessage authMessage = auth.createAuthenticationMessage();

    auth2.own = ptp2.getIdentifier();
    auth2.other = ptp1.getIdentifier();

    assertEquals(false, auth2.authenticationMessageValid(authMessage));
  }

  @Test
  public void testChangeSource() throws IOException, GeneralSecurityException {
    auth.own = new Identifier("aaaaaaaaaaaaaaaa.onion");
    auth.other = ptp2.getIdentifier();
    AuthenticationMessage authMessage = auth.createAuthenticationMessage();

    authMessage.source = ptp1.getIdentifier();

    auth2.own = ptp2.getIdentifier();
    auth2.other = ptp1.getIdentifier();

    assertEquals(false, auth2.authenticationMessageValid(authMessage));
  }

  @Test
  public void testInvalidSourceIdentifier() throws IOException, GeneralSecurityException {
    auth.own = new Identifier("xyz");
    auth.other = ptp2.getIdentifier();
    AuthenticationMessage authMessage = auth.createAuthenticationMessage();

    auth2.own = ptp2.getIdentifier();
    auth2.other = ptp1.getIdentifier();

    assertEquals(false, auth2.authenticationMessageValid(authMessage));
  }

  @Test
  public void testInvalidDestinationIdentifier() throws IOException, GeneralSecurityException {
    auth.own = ptp1.getIdentifier();
    auth.other = new Identifier("xyz");
    AuthenticationMessage authMessage = auth.createAuthenticationMessage();

    auth2.own = ptp2.getIdentifier();
    auth2.other = ptp1.getIdentifier();

    assertEquals(false, auth2.authenticationMessageValid(authMessage));
  }


  @Test
  public void testWrongDestination() throws IOException, GeneralSecurityException {
    auth.own = ptp1.getIdentifier();
    auth.other = new Identifier("aaaaaaaaaaaaaaaa.onion");
    AuthenticationMessage authMessage = auth.createAuthenticationMessage();

    auth2.own = ptp2.getIdentifier();

    assertEquals(false, auth2.authenticationMessageValid(authMessage));
  }

  @Test
  public void testChangeDestination() throws GeneralSecurityException, UnsupportedEncodingException {
    auth.own = ptp1.getIdentifier();
    auth.other = new Identifier("aaaaaaaaaaaaaaaa.onion");
    AuthenticationMessage authMessage = auth.createAuthenticationMessage();

    authMessage.destination = ptp2.getIdentifier();

    auth2.own = ptp2.getIdentifier();

    assertEquals(false, auth2.authenticationMessageValid(authMessage));
  }

  @Test
  public void testOldTimestamp() throws IOException, GeneralSecurityException {
    auth.own = ptp1.getIdentifier();
    auth.other = ptp2.getIdentifier();

    // 4 minutes old authenticator
    AuthenticationMessage authMessage =
        auth.createAuthenticationMessage(System.currentTimeMillis() - 4 * 60 * 1000);

    auth2.own = ptp2.getIdentifier();

    assertEquals(false, auth2.authenticationMessageValid(authMessage));
  }

  @Test
  public void testFutureTimestamp() throws IOException, GeneralSecurityException {
    auth.own = ptp1.getIdentifier();
    auth.other = ptp2.getIdentifier();

    // timestamp 4 minutes in the future
    AuthenticationMessage authMessage =
        auth.createAuthenticationMessage(System.currentTimeMillis() + 4 * 60 * 1000);

    auth2.own = ptp2.getIdentifier();

    assertEquals(false, auth2.authenticationMessageValid(authMessage));
  }


  @Test
  public void testGetBytes() throws UnsupportedEncodingException {
    Identifier source = ptp1.getIdentifier();
    Identifier destination = ptp2.getIdentifier();

    byte[] pubKey = cryptHelper.getPublicKeyBytes();
    // Set all bits to make sure no bits get lost at the end
    long timestamp = Long.MAX_VALUE;

    int sourceLen = source.toString().getBytes(Constants.charset).length;
    int destinationLen = destination.toString().getBytes(Constants.charset).length;
    int pubKeyLen = pubKey.length;

    ByteBuffer buffer = auth.getBytes(source, destination, pubKey, timestamp);

    byte[] sourceEncoded = new byte[sourceLen];
    byte[] destinationEncoded = new byte[destinationLen];
    byte[] pubKeyEncoded = new byte[pubKeyLen];
    long timestampEncoded;

    int offset = 0;

    buffer.get(sourceEncoded, offset, sourceLen);
    buffer.get(destinationEncoded, offset, destinationLen);
    buffer.get(pubKeyEncoded, offset, pubKeyLen);
    timestampEncoded = buffer.getLong();

    Identifier source2 = new Identifier(new String(sourceEncoded, Constants.charset));
    Identifier destination2 = new Identifier(new String(destinationEncoded, Constants.charset));

    assertEquals(source, source2);
    assertEquals(destination, destination2);
    assertArrayEquals(pubKey, pubKeyEncoded);
    assertEquals(timestamp, timestampEncoded);
  }

  @Test
  public void testChangePublicKey() throws GeneralSecurityException, UnsupportedEncodingException {
    auth.own = ptp1.getIdentifier();
    auth.other = ptp2.getIdentifier();
    AuthenticationMessage authMessage = auth.createAuthenticationMessage();

    authMessage.pubKey = cryptHelper2.getPublicKeyBytes();

    auth2.own = ptp2.getIdentifier();

    assertEquals(false, auth2.authenticationMessageValid(authMessage));
  }

  /**
   * Test to check the padding of the signatures. Right now we use PKCS #1 v1.5 with SHA256.
   * If that is changed the test fails of course. The test decrypts the signature with
   * the public key and only checks if it ends with the right hash.
   * The test is also helpful for manual investigation of the signature.
   */
  @Test
  public void testVerifyPKCS15Padding() throws GeneralSecurityException, UnsupportedEncodingException {
    Identifier source = ptp1.getIdentifier();
    Identifier destination = ptp2.getIdentifier();
    long timestamp = System.currentTimeMillis();

    ByteBuffer tosign = auth.getBytes(source, destination, cryptHelper.getPublicKeyBytes(),
        timestamp);

    // Hash message
    MessageDigest sha256 = MessageDigest.getInstance("SHA256");
    byte[] hash = sha256.digest(tosign.array());

    // Sign message
    byte[] signature = cryptHelper.sign(tosign);

    Cipher c = Cipher.getInstance("RSA/ECB/NoPadding");
    // Encryption with public key decrypts signature
    c.init(Cipher.ENCRYPT_MODE, cryptHelper.decodePublicKey(cryptHelper.getPublicKeyBytes()));

    // Decrypt signature with public key
    byte[] decrypted = c.doFinal(signature);

    byte[] decryptedHash = new byte[hash.length];

    // Copy hash out of decrypted data
    System.arraycopy(decrypted, decrypted.length - hash.length, decryptedHash, 0, hash.length);

    /* // Use to examine signature
    System.out.println("Bytes to sign (length " + tosign.array().length + ")");
    System.out.println(toHex(tosign.array()));

    System.out.println("SHA256 Hash (length " + hash.length + ")");
    System.out.println(toHex(hash));

    System.out.println("Signature (length " + signature.length + ")");
    System.out.println(toHex(signature));

    System.out.println("Decrypted Signature (length " + decrypted.length + ")");
    System.out.println(toHex(decrypted));

    System.out.println("Decrypted Hash (length " + decryptedHash.length + ")");
    System.out.println(toHex(decryptedHash));*/

    assertArrayEquals(hash, decryptedHash);
  }

  private String toHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      sb.append(String.format("%02X ", b));
    }
    return sb.toString();
  }
}
