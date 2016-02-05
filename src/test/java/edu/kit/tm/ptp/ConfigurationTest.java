package edu.kit.tm.ptp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import edu.kit.tm.ptp.Configuration;
import edu.kit.tm.ptp.utility.Constants;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Random;

/**
 * This class offers JUnit testing for the Configuration class.
 *
 * @author Simeon Andreev
 *
 */
public class ConfigurationTest {

  /** New line constant. */
  private static final String newline = "\n";
  /** The prefix used for the temporary file in which the configuration properties are written. */
  private static final String prefix = "ConfigurationTest";
  /** The suffix used for the temporary file in which the configuration properties are written. */
  private static final String suffix = "junit";

  /** The input file for the configuration. */
  private File file = null;
  /** The configuration used for the test. */
  private Configuration configuration = null;
  /** The properties used for the test. */
  private String defaultIdentifier = null;
  private String hiddenServiceDirectory = null;
  private int hiddenServicePort = -1;
  private byte[] authenticationBytes = null;
  private int torBootstrapTimeout = -1;
  private int torControlPort = -1;
  private int torSocksProxyPort = -1;
  private int socketTimeout = -1;
  private int socketReceivePoll = -1;
  private int socketTtl = -1;
  private int ttlPoll;


  /**
   * @throws IOException
   *
   * @see JUnit
   */
  @Before
  public void setUp() throws IOException {
    file = File.createTempFile(prefix, suffix);

    // Set the default identifier.
    defaultIdentifier = "defid";
    // Set the hidden service directory.
    hiddenServiceDirectory = Paths.get("").toString() + File.separator + Constants.hiddenservicedir;
    // Set the authentication bytes.
    authenticationBytes = new byte[] {};

    // Create the RNG.
    Random random = new Random();

    // Choose random property values.
    torControlPort = random.nextInt();
    torSocksProxyPort = random.nextInt();
    hiddenServicePort = random.nextInt();
    torBootstrapTimeout = random.nextInt();
    socketTimeout = random.nextInt();
    socketReceivePoll = random.nextInt();
    socketTtl = random.nextInt();
    ttlPoll = random.nextInt();

    // Write the properties to the input file.
    FileWriter writer = new FileWriter(file);

    System.out.println(ConfigurationFileReader.HiddenServicePort + " " + hiddenServicePort);
    System.out
        .println(ConfigurationFileReader.SocketConnectTimeout + " " + socketTimeout + newline);
    System.out.println(ConfigurationFileReader.SocketTTL + " " + socketTtl + newline);
    System.out.println(ConfigurationFileReader.SocketTTLPoll + " " + ttlPoll + newline);

    BufferedWriter output = new BufferedWriter(writer);
    output.write(ConfigurationFileReader.DefaultIdentifier + " " + defaultIdentifier + newline);
    output.write(ConfigurationFileReader.HiddenServicePort + " " + hiddenServicePort + newline);
    output.write(ConfigurationFileReader.TorBootstrapTimeout + " " + torBootstrapTimeout + newline);
    output.write(ConfigurationFileReader.SocketConnectTimeout + " " + socketTimeout + newline);
    output.write(ConfigurationFileReader.SocketReceivePoll + " " + socketReceivePoll + newline);
    output.write(ConfigurationFileReader.SocketTTL + " " + socketTtl + newline);
    output.write(ConfigurationFileReader.SocketTTLPoll + " " + ttlPoll + newline);

    output.flush();
    output.close();

    ConfigurationFileReader reader = new ConfigurationFileReader(file.getCanonicalPath());
    // Create the configuration.
    configuration = reader.readFromFile();
    configuration.setTorConfiguration(Paths.get("").toString(), torControlPort, torSocksProxyPort);
  }

  /**
   * @see JUnit
   */
  @After
  public void tearDown() {
    // Delete the file containing the properties.
    file.delete();
    assertFalse(file.exists());
  }


  /**
   * Test method for {@link edu.kit.tm.ptp.Configuration#getDefaultIdentifier()}.
   *
   * <p>
   * Checks whether the configuration read the hidden service directory property correctly. Fails
   * iff the read property is not equal to the written property.
   */
  @Test
  public void testGetDefaultIdentifier() {
    System.out.println(configuration.getDefaultIdentifier());
    if (!defaultIdentifier.equals(configuration.getDefaultIdentifier())) {
      fail("Default identifier property does not match: " + defaultIdentifier + " != "
          + configuration.getDefaultIdentifier());
    }
  }

  /**
   * Test method for {@link edu.kit.tm.ptp.Configuration#getHiddenServiceDirectory()}.
   *
   * <p>
   * Checks whether the configuration read the hidden service directory property correctly. Fails if
   * the read property is not equal to the written property.
   */
  @Test
  public void testGetHiddenServiceDirectory() {
    System.out.println(configuration.getHiddenServiceDirectory());
    if (!hiddenServiceDirectory.equals(configuration.getHiddenServiceDirectory())) {
      fail("Hidden service directory property does not match: " + hiddenServiceDirectory + " != "
          + configuration.getHiddenServiceDirectory());
    }
  }

  /**
   * Test method for {@link edu.kit.tm.ptp.Configuration#getHiddenServicePort()}.
   *
   * <p>
   * Checks whether the configuration read the hidden service port number property correctly. Fails
   * if the read property is not equal to the written property.
   */
  @Test
  public void testGetHiddenServicePort() {
    if (hiddenServicePort != configuration.getHiddenServicePort()) {
      fail("Hidden service port property does not match: " + hiddenServicePort + " != "
          + configuration.getHiddenServicePort());
    }
  }

  /**
   * Test method for {@link edu.kit.tm.ptp.Configuration#getTorBootstrapTimeout()}.
   *
   * <p>
   * Checks whether the configuration read the Tor bootstrap timeout property correctly. Fails if
   * the read property is not equal to the written property.
   */
  @Test
  public void testGetTorBootstrapTimeout() {
    if (torBootstrapTimeout != configuration.getTorBootstrapTimeout()) {
      fail("Bootstrap timeout property does not match: " + torBootstrapTimeout + " != "
          + configuration.getTorBootstrapTimeout());
    }
  }

  /**
   * Test method for {@link edu.kit.tm.ptp.Configuration#getAuthenticationBytes()}.
   *
   * <p>
   * Checks whether the configuration read the authentication bytes property correctly. Fails if the
   * read property is not equal to the written property.
   */
  @Test
  public void testGetAuthenticationBytes() {
    if (!Arrays.equals(authenticationBytes, configuration.getAuthenticationBytes())) {
      fail("Authentication bytes property does not match: " + Arrays.toString(authenticationBytes)
          + " != " + Arrays.toString(configuration.getAuthenticationBytes()));
    }
  }

  /**
   * Test method for {@link edu.kit.tm.ptp.Configuration#getTorControlPort()}.
   *
   * <p>
   * Checks whether the configuration read the Tor control port number property correctly. Fails if
   * the read property is not equal to the written property.
   */
  @Test
  public void testGetTorControlPort() {
    if (torControlPort != configuration.getTorControlPort()) {
      fail("Tor control port property does not match: " + torControlPort + " != "
          + configuration.getTorControlPort());
    }
  }

  /**
   * Test method for {@link edu.kit.tm.ptp.Configuration#getTorSOCKSProxyPort()}.
   *
   * <p>
   * Checks whether the configuration read the SOCKS proxy port number property correctly. Fails iff
   * the read property is not equal to the written property.
   */
  @Test
  public void testGetTorSOCKSProxyPort() {
    if (torSocksProxyPort != configuration.getTorSOCKSProxyPort()) {
      fail("Tor SOCKS proxy port property does not match: " + torSocksProxyPort + " != "
          + configuration.getTorSOCKSProxyPort());
    }
  }

  /**
   * Test method for {@link edu.kit.tm.ptp.Configuration#getSocketTimeout()}.
   *
   * <p>
   * Checks whether the configuration read the socket timeout property correctly. Fails iff the read
   * property is not equal to the written property.
   */
  @Test
  public void testGetSocketTimeout() {
    if (socketTimeout != configuration.getSocketTimeout()) {
      fail("Socket timeout property does not match: " + socketTimeout + " != "
          + configuration.getSocketTimeout());
    }
  }

  /**
   * Test method for {@link edu.kit.tm.ptp.Configuration#getSocketTTL()}.
   *
   * <p>
   * Checks whether the configuration read the socket TTL property correctly. Fails iff the read
   * property is not equal to the written property.
   */
  @Test
  public void testGetSocketTTL() {
    if (socketTtl != configuration.getSocketTTL()) {
      fail("Socket TTL property does not match: " + socketTtl + " != "
          + configuration.getSocketTTL());
    }
  }

  /**
   * Test method for {@link edu.kit.tm.ptp.Configuration#getTTLPoll()}.
   *
   * <p>
   * Checks whether the configuration read the TTL poll property correctly. Fails iff the read
   * property is not equal to the written property.
   */
  @Test
  public void testGetTTLPoll() {
    if (ttlPoll != configuration.getTTLPoll()) {
      fail("TTL poll property does not match: " + ttlPoll + " != " + configuration.getTTLPoll());
    }
  }

}
