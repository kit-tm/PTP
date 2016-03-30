package edu.kit.tm.ptp.auth;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.spec.InvalidKeySpecException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.kit.tm.ptp.Identifier;
import edu.kit.tm.ptp.PTP;
import edu.kit.tm.ptp.auth.PublicKeyAuthenticator.AuthenticationMessage;
import edu.kit.tm.ptp.crypt.CryptHelper;
import edu.kit.tm.ptp.utility.Constants;

public class PublicKeyAuthenticatorTest {
  private PublicKeyAuthenticator auth;
  private PublicKeyAuthenticator auth2;
  private static PTP ptp1;
  private static PTP ptp2;
  private static File pk1;
  private static File pk2;

  @BeforeClass
  public static void setUpClass() throws IOException {
    ptp1 = new PTP();
    ptp2 = new PTP();

    ptp1.init();
    ptp2.init();

    ptp1.reuseHiddenService();
    ptp2.reuseHiddenService();

    String hsDir1 = ptp1.getHiddenServiceDirectory();
    String hsDir2 = ptp2.getHiddenServiceDirectory();

    pk1 = new File(hsDir1 + File.separator + Constants.prkey);
    pk2 = new File(hsDir2 + File.separator + Constants.prkey);
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
    ptp1.exit();
    ptp2.exit();
  }

  @Before
  public void setUp() throws Exception {
    auth = new PublicKeyAuthenticator(null, null, null);
    auth2 = new PublicKeyAuthenticator(null, null, null);
  }

  @After
  public void tearDown() throws Exception {}

  @Test
  public void testAuthenticator() throws IOException, GeneralSecurityException {
    testAuthenticator(false);
  }

  @Test
  public void testAuthenticatorUnknown() throws IOException, GeneralSecurityException {
    testAuthenticator(true);
  }

  private void testAuthenticator(boolean unknown) throws IOException, GeneralSecurityException {
    CryptHelper helper = CryptHelper.getInstance();
    helper.setKeyPair(helper.readKeyPairFromFile(pk1));

    auth.own = ptp1.getIdentifier();
    auth.other = ptp2.getIdentifier();
    AuthenticationMessage authMessage = auth.createAuthenticator();

    helper.setKeyPair(helper.readKeyPairFromFile(pk2));

    auth2.own = ptp2.getIdentifier();
    auth2.other = unknown ? null : ptp1.getIdentifier();

    assertEquals(true, auth2.authenticatorValid(authMessage));
  }

  @Test
  public void testWrongAuthenticator() throws IOException, GeneralSecurityException {
    CryptHelper helper = CryptHelper.getInstance();
    helper.setKeyPair(helper.readKeyPairFromFile(pk1));

    auth.own = ptp1.getIdentifier();
    auth.other = ptp2.getIdentifier();
    AuthenticationMessage authMessage = auth.createAuthenticator();
    byte[] signature = authMessage.signature;

    // flip bits of first byte
    signature[0] = (byte) (signature[0] ^ 0xff);

    authMessage.signature = signature;

    helper.setKeyPair(helper.readKeyPairFromFile(pk2));

    auth2.own = ptp2.getIdentifier();

    assertEquals(false, auth2.authenticatorValid(authMessage));
  }

  @Test
  public void testWrongSource() throws IOException, GeneralSecurityException {
    CryptHelper helper = CryptHelper.getInstance();
    helper.setKeyPair(helper.readKeyPairFromFile(pk1));

    auth.own = new Identifier("aaaaaaaaaaaaaaaa.onion");
    auth.other = ptp2.getIdentifier();
    AuthenticationMessage authMessage = auth.createAuthenticator();

    helper.setKeyPair(helper.readKeyPairFromFile(pk2));

    auth2.own = ptp2.getIdentifier();
    auth2.other = ptp1.getIdentifier();

    assertEquals(false, auth2.authenticatorValid(authMessage));
  }

  @Test
  public void testInvalidSourceIdentifier() throws IOException, GeneralSecurityException {
    CryptHelper helper = CryptHelper.getInstance();
    helper.setKeyPair(helper.readKeyPairFromFile(pk1));

    auth.own = new Identifier("xyz");
    auth.other = ptp2.getIdentifier();
    AuthenticationMessage authMessage = auth.createAuthenticator();

    helper.setKeyPair(helper.readKeyPairFromFile(pk2));

    auth2.own = ptp2.getIdentifier();
    auth2.other = ptp1.getIdentifier();

    assertEquals(false, auth2.authenticatorValid(authMessage));
  }

  @Test
  public void testInvalidDestinationIdentifier() throws IOException, GeneralSecurityException {
    CryptHelper helper = CryptHelper.getInstance();
    helper.setKeyPair(helper.readKeyPairFromFile(pk1));

    auth.own = ptp1.getIdentifier();
    auth.other = new Identifier("xyz");
    AuthenticationMessage authMessage = auth.createAuthenticator();

    helper.setKeyPair(helper.readKeyPairFromFile(pk2));

    auth2.own = ptp2.getIdentifier();
    auth2.other = ptp1.getIdentifier();

    assertEquals(false, auth2.authenticatorValid(authMessage));
  }


  @Test
  public void testWrongDestination() throws IOException, GeneralSecurityException {
    CryptHelper helper = CryptHelper.getInstance();
    helper.setKeyPair(helper.readKeyPairFromFile(pk1));

    auth.own = ptp1.getIdentifier();
    auth.other = new Identifier("aaaaaaaaaaaaaaaa.onion");
    AuthenticationMessage authMessage = auth.createAuthenticator();

    helper.setKeyPair(helper.readKeyPairFromFile(pk2));

    auth2.own = ptp2.getIdentifier();

    assertEquals(false, auth2.authenticatorValid(authMessage));
  }

  @Test
  public void testUseAuthTwoTimes() throws IOException, GeneralSecurityException {
    CryptHelper helper = CryptHelper.getInstance();
    helper.setKeyPair(helper.readKeyPairFromFile(pk1));

    auth.own = ptp1.getIdentifier();
    auth.other = ptp2.getIdentifier();
    AuthenticationMessage authMessage = auth.createAuthenticator();

    helper.setKeyPair(helper.readKeyPairFromFile(pk2));

    auth2.own = ptp2.getIdentifier();

    assertEquals(true, auth2.authenticatorValid(authMessage));
    assertEquals(false, auth2.authenticatorValid(authMessage));
  }

  @Test
  public void testOldTimestamp() throws IOException, GeneralSecurityException {
    CryptHelper helper = CryptHelper.getInstance();
    helper.setKeyPair(helper.readKeyPairFromFile(pk1));

    auth.own = ptp1.getIdentifier();
    auth.other = ptp2.getIdentifier();

    // 2 minutes old authenticator
    AuthenticationMessage authMessage =
        auth.createAuthenticator(System.currentTimeMillis() - 120 * 1000);

    helper.setKeyPair(helper.readKeyPairFromFile(pk2));

    auth2.own = ptp2.getIdentifier();

    assertEquals(false, auth2.authenticatorValid(authMessage));
  }

  @Test
  public void testFutureTimestamp() throws IOException, GeneralSecurityException {
    CryptHelper helper = CryptHelper.getInstance();
    helper.setKeyPair(helper.readKeyPairFromFile(pk1));

    auth.own = ptp1.getIdentifier();
    auth.other = ptp2.getIdentifier();

    // timestamp 2 minutes in the future
    AuthenticationMessage authMessage =
        auth.createAuthenticator(System.currentTimeMillis() + 120 * 1000);

    helper.setKeyPair(helper.readKeyPairFromFile(pk2));

    auth2.own = ptp2.getIdentifier();

    assertEquals(false, auth2.authenticatorValid(authMessage));
  }


}
