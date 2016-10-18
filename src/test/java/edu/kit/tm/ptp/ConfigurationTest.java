package edu.kit.tm.ptp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import edu.kit.tm.ptp.utility.Constants;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
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
  private String hiddenServicesDirectory = null;
  private int hiddenServicePort = -1;
  private byte[] authenticationBytes = null;
  private int torControlPort = -1;
  private int torSocksProxyPort = -1;
  private int timerUpdateInterval;
  private int isAliveTimeout = -1;
  private int isAliveSendTimeout = -1;


  /**
   * @throws IOException
   *
   * @see JUnit
   */
  @Before
  public void setUp() throws IOException {
    file = File.createTempFile(prefix, suffix);

    // Set the hidden service directory.
    hiddenServicesDirectory =
        Paths.get("").toString() + File.separator + Constants.hiddenservicedir;
    // Set the authentication bytes.
    authenticationBytes = new byte[] {};

    // Create the RNG.
    Random random = new Random();

    // Choose random property values.
    torControlPort = random.nextInt();
    torSocksProxyPort = random.nextInt();
    hiddenServicePort = random.nextInt();
    isAliveTimeout = random.nextInt();
    isAliveSendTimeout = random.nextInt();
    timerUpdateInterval = random.nextInt();

    // Write the properties to the input file.
    BufferedWriter writer = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(file), Constants.charset));

    BufferedWriter output = new BufferedWriter(writer);
    output.write(ConfigurationFileReader.HiddenServicePort + " " + hiddenServicePort + newline);
    output.write(ConfigurationFileReader.IsAliveTimeout + " " + isAliveTimeout + newline);
    output.write(ConfigurationFileReader.IsAliveSendTimeout + " " + isAliveSendTimeout + newline);
    output.write(ConfigurationFileReader.TimerUpdateInterval + " " + timerUpdateInterval + newline);

    output.flush();
    output.close();

    ConfigurationFileReader reader = new ConfigurationFileReader(file.getCanonicalPath());
    // Create the configuration.
    configuration = reader.readFromFile();
    configuration.setWorkingDirectory(Paths.get("").toString());
    configuration.setHiddenServicesDirectory(hiddenServicesDirectory);
    configuration.setTorControlPort(torControlPort);
    configuration.setTorSocksProxyPort(torSocksProxyPort);
  }

  /**
   * @see JUnit
   */
  @After
  public void tearDown() {
    // Delete the file containing the properties.
    assertTrue(file.delete());
    assertFalse(file.exists());
  }

  /**
   * Test method for {@link edu.kit.tm.ptp.Configuration#getHiddenServicesDirectory()}.
   *
   * <p>
   * Checks whether the configuration read the hidden service directory property correctly. Fails if
   * the read property is not equal to the written property.
   */
  @Test
  public void testGetHiddenServicesDirectory() {
    System.out.println(configuration.getHiddenServicesDirectory());
    if (!hiddenServicesDirectory.equals(configuration.getHiddenServicesDirectory())) {
      fail("Hidden service directory property does not match: " + hiddenServicesDirectory + " != "
          + configuration.getHiddenServicesDirectory());
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
   * Test method for {@link Configuration#getIsAliveTimeout()}.
   *
   * <p>
   * Checks whether the configuration read the IsAliveTimeout property correctly. Fails iff the read
   * property is not equal to the written property.
   */
  @Test
  public void testGetIsAliveTimeout() {
    if (isAliveTimeout != configuration.getIsAliveTimeout()) {
      fail("IsAliveTimeout property does not match: " + isAliveTimeout + " != "
          + configuration.getIsAliveTimeout());
    }
  }

  /**
   * Test method for {@link Configuration#getIsAliveSendTimeout()}.
   *
   * <p>
   * Checks whether the configuration read the IsAliveSendTimeout property correctly.
   * Fails iff the read property is not equal to the written property.
   */
  @Test
  public void testGetIsSendAliveTimeout() {
    if (isAliveSendTimeout != configuration.getIsAliveSendTimeout()) {
      fail("IsAliveSendTimeout property does not match: " + isAliveSendTimeout + " != "
          + configuration.getIsAliveSendTimeout());
    }
  }

  /**
   * Test method for {@link edu.kit.tm.ptp.Configuration#getTimerUpdateInterval()}.
   *
   * <p>
   * Checks whether the configuration read the timerUpdateInterval property correctly. Fails iff the read
   * property is not equal to the written property.
   */
  @Test
  public void testGetTimerUpdateInterval() {
    if (timerUpdateInterval != configuration.getTimerUpdateInterval()) {
      fail("TTL poll property does not match: " + timerUpdateInterval + " != " + configuration.getTimerUpdateInterval());
    }
  }

}
