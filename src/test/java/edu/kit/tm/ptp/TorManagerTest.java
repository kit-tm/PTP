package edu.kit.tm.ptp;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class TorManagerTest {
  private TorManager manager;
  private TorManager manager2;
  private static final long bootstrapTimeout = 120 * 1000;
  
  @Before
  public void setUp() throws Exception {
    manager = new TorManager();
    manager2 = new TorManager();
  }

  @After
  public void tearDown() throws Exception {
    manager.stopTor();
    manager2.stopTor();
  }

  @Test
  public void testStartAndWait() throws IOException, InterruptedException {
    manager.startTor();
    assertEquals(true, manager.torRunning());
    manager.waitForBootstrapping(bootstrapTimeout);
    assertEquals(true, manager.torBootstrapped());
  }
  
  @Test
  public void testSharedTorProcess() throws IOException, InterruptedException {
    manager.startTor();
    assertEquals(true, manager.torRunning());
    manager.waitForBootstrapping(bootstrapTimeout);
    assertEquals(true, manager.torBootstrapped());
    
    manager2.startTor();
    assertEquals(true, manager2.torRunning());
    // Manager2 shouldn't need to bootstrap
    assertEquals(true, manager2.torBootstrapped());
    
    // The managers should share the same tor process
    assertEquals(manager.getTorControlPort(), manager2.getTorControlPort());
    assertEquals(manager.getTorSOCKSPort(), manager2.getTorSOCKSPort());
  }
  
  @Test
  public void testStopTor() throws IOException {
    manager.startTor();
    manager.stopTor();
    assertEquals(false, manager.torRunning());
  }
  
  /*
   * There shouldn't be another Tor process running in the directory.
   */
  @Test (expected = IllegalStateException.class)
  public void testGetTorControlPortNoBootstrap() throws IOException {
    manager.startTor();
    manager.getTorControlPort();
  }
  
  /*
   * There shouldn't be another Tor process running in the directory.
   */
  @Test (expected = IllegalStateException.class)
  public void testGetTorSOCKSPortNoBootstrap() throws IOException {
    manager.startTor();
    manager.getTorSOCKSPort();
  }

}
