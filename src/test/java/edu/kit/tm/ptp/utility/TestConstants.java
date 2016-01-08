package edu.kit.tm.ptp.utility;

/**
 * Class containing some constants used throughout the tests.
 * 
 * @author Timon Hackenjos
 *
 */
public class TestConstants {
  
  /** Timeout for a socket connection. **/
  public static final int socketConnectTimeout = 10 * 1000;
  
  /** Timeout for a hidden service to become accessible through the tor network. **/
  public static final long hiddenServiceSetupTimeout = 180 * 1000;
  
  /** Timeout for a listener to reflect an event that occurred. **/
  public static final long listenerTimeout = 5 * 1000;
}
