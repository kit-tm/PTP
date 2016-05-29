package edu.kit.tm.ptp.connection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import edu.kit.tm.ptp.Identifier;
import edu.kit.tm.ptp.connection.ExpireListener;
import edu.kit.tm.ptp.connection.TTLManager;
import edu.kit.tm.ptp.utility.Constants;
import edu.kit.tm.ptp.utility.RNG;
import edu.kit.tm.ptp.utility.TestHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * This class offers JUnit testing for the TTLManager class.
 *
 * @author Simeon Andreev
 *
 */
public class TTLManagerTest {


  /**
   * A dummy listener for the TTLManager, used to test TTL expiration.
   *
   * @author Simeon Andreev
   *
   */
  private class Client implements ExpireListener {

    /** An atomic boolean used to check whether an expiration timer has been reached. */
    public AtomicBoolean disconnected = new AtomicBoolean(false);

    /**
     * @see TTLManager.Listener
     */
    @Override
    public void expired(Identifier identifier) {
      disconnected.set(true);
    }

  }


  /** The interval (in milliseconds) in which the TTLManager should check TTLs. */
  private static final int step = 50;
  /** Minimum random identifier length used in the set test. */
  private static final int minimumIdentifierLength = 10;
  /** Maximum random identifier length used in the set test. */
  private static final int maximumIdentifierLength = 50;
  /** Minimum random expiration time used in the set test. */
  private static final int minimumExpirationTime = 100;
  /** Maximum random expiration time used in the set test. */
  private static final int maximumExpirationTime = 200;

  /** The identifier used in the set test. */
  private Identifier identifier = null;
  /** The expiration time used in the set test. */
  private int expiration = -1;
  /**
   * The TTLManager listener used to check whether the TTLManager expire notifications are correct.
   */
  private Client client = null;
  /** The running TTLManager. */
  private TTLManager runningManager = null;
  /** The not started TTLManager. */
  private TTLManager manager = null;


  /**
   * @see JUnit
   */
  @Before
  public void setUp() throws IOException {
    // Create a RNG.
    RNG random = new RNG();
    
    Charset charset = Charset.forName(Constants.charset);

    // Create the random identifier within the given length bounds.
    String randomString = random.string(minimumIdentifierLength, maximumIdentifierLength, charset);
    identifier = new Identifier(randomString);

    // Set the expiration timer. Choose a random number within the expiration time bounds.
    expiration = random.integer(minimumExpirationTime, maximumExpirationTime);

    // Create the TTLManager listener.
    client = new Client();
    // Create and start the running TTLManager.
    runningManager = new TTLManager(client, step);
    runningManager.start();
    // Create the not started TTLManager.
    manager = new TTLManager(new Client(), step);

    // Wait for the running TTLManager threads to enter their execution loops.
    try {
      Thread.sleep(50);
    } catch (InterruptedException e) {
      // Sleeping was interrupted. Do nothing.
    }

    // Add the random identifier to the running TTLManager.
    runningManager.put(identifier);
  }

  /**
   * @see JUnit
   */
  @After
  public void tearDown() {
    runningManager.stop();
  }

  /**
   * Test method for {@link edu.kit.tm.ptp.connection.TTLManager#set(java.lang.String, int)}.
   * Checks whether an expire notification is sent for the random identifier by the TTLManager.
   * Fails iff the notififaction is not received shorty after the expiration time.
   */
  @Test
  public void testSet() {
    // Set the TTL of the identifier.
    runningManager.set(identifier, expiration);

    // Wait for the TTL to expire.
    final long start = System.currentTimeMillis();
    while (!client.disconnected.get()
        && System.currentTimeMillis() - start < step + expiration + 50) {
      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
        // Sleeping was interrupted. Do nothing.
      }
    }

    // Check if the listener was notified of the expiration.
    assertTrue("Listener was not notified of the expired TTL.", client.disconnected.get());
  }

  /**
   * Test method for {@link edu.kit.tm.ptp.thread.Suspendable#running()}.
   * Checks whether the running TTLManager, the not started TTLManager and the stopped running
   * TTLManager tell their states correctly.
   * Fails iff any of the returned states are incorrect.
   */
  @Test
  public void testRunning() {
    // Check if the running TTLManager tells its state correctly.
    if (!runningManager.isRunning()) {
      fail("Started TTLManager running check returns false.");
    }

    // Check if the not started TTLManager tells its state correctly.
    if (manager.isRunning()) {
      fail("Not started TTLManager running check returns true.");
    }

    // Stop the running manager.
    runningManager.stop();

    // Check if the stopped running TTLManager tells its state correctly.
    assertFalse("Stopped TTLManager running check returns true.", runningManager.isRunning());
  }

}
