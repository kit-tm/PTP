package edu.kit.tm.ptp;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import edu.kit.tm.ptp.TorManager.SOCKSProxyListener;
import edu.kit.tm.ptp.utility.Constants;
import edu.kit.tm.ptp.utility.TestConstants;
import edu.kit.tm.ptp.utility.TestHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class TorManagerTest {
  private TorManager torManager;

  @Before
  public void setUp() throws Exception {
    torManager = new SharedTorManager(Constants.ptphomedefault, null);
  }

  @After
  public void tearDown() throws Exception {
    if (torManager != null) {
      torManager.stopTor();
    }
  }
  
  private static final class MySOCKSListener implements SOCKSProxyListener {
    public String socksHost;
    public int sockProxyPort;
    public AtomicBoolean updated = new AtomicBoolean(false);

    @Override
    public void updateSOCKSProxy(String socksHost, int socksProxyPort) {
      this.socksHost = socksHost;
      this.sockProxyPort = socksProxyPort;
      this.updated.set(true);
    }
    
  }

  @Test
  public void testStartStop() {
    assertTrue(torManager.startTor());
    assertTrue(torManager.torRunning());
    
    assertNotEquals(-1, torManager.getTorControlPort());
    assertNotEquals(-1, torManager.getTorSOCKSPort());
    
    torManager.stopTor();
  }
  
  @Test
  public void testSOCKSProxyListener() {
    MySOCKSListener listener = new MySOCKSListener();
    torManager.addSOCKSProxyListener(listener);
    
    assertTrue(torManager.startTor());
    
    TestHelper.wait(listener.updated, TestConstants.listenerTimeout);
    
    assertTrue(listener.updated.get());
    
    assertNotNull(listener.socksHost);
    assertNotEquals(-1, listener.sockProxyPort);
    
    torManager.stopTor();
  }
  
  @Test
  public void closeCircuitInvalidIdentifier() {
    assertTrue(torManager.startTor());
    
    Identifier invalid = new Identifier("foo.bar");
    
    torManager.closeCircuits(invalid);
    
    torManager.stopTor();
  }
  
  @Test
  public void changeNetwork() {
    MySOCKSListener listener = new MySOCKSListener();
    
    assertTrue(torManager.startTor());
    
    torManager.changeNetwork(false);
    
    torManager.addSOCKSProxyListener(listener);
    torManager.changeNetwork(true);
    
    TestHelper.wait(listener.updated, TestConstants.listenerTimeout);
    
    assertTrue(listener.updated.get());
    
    assertNotNull(listener.socksHost);
    assertNotEquals(-1, listener.sockProxyPort);
    
    torManager.stopTor();
  }
}
