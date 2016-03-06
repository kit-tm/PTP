package edu.kit.tm.ptp.examples;

import edu.kit.tm.ptp.Identifier;
import edu.kit.tm.ptp.MessageReceivedListener;
import edu.kit.tm.ptp.PTP;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


/**
 * Ping example for the PTP API.
 *
 * @author Martin Florian, Simeon Andreev
 *
 */
public class PTPPingExample {
 
  /*
   * Interval between two pings (in ms).
   */
  private static final long pingInterval = 10 * 1000;
  
  private static long pingStartTime;

  private static PTP ptp;
  
  /**
   * Message type for pings.
   */
  static class PingMessage {
    int seq; // sequence number
    
    // no-arg constructor required for PTP 
    public PingMessage() {
      this(0);
    }
    
    public PingMessage(int seq) {
      this.seq = seq;
    }
  }
  
  /**
   * Message type for ping replies.
   */
  static class PongMessage {
    int seq; // sequence number
    
    // no-arg constructor required for PTP 
    public PongMessage() {
      this(0);
    }
    
    public PongMessage(int seq) {
      this.seq = seq;
    }
  }
   
  private static void sendPing(int seq, Identifier destination) {
    ptp.sendMessage(new PingMessage(seq), destination);
  }
  
  private static void sendPong(int seq, Identifier destination) {
    ptp.sendMessage(new PongMessage(seq), destination);
  }
  
  /**
   * The main method.
   *
   * @param args Program parameters, ignored.
   */
  public static void main(String[] args) {
    
    // Create PTP object.
    ptp = new PTP();

    try {
      // Initialize
      System.out.print("Initializing PTP...");
      ptp.init();
      System.out.println(" done.");

      // Setup Identifier
      ptp.createHiddenService();
      System.out.println("Created hidden service.");

      // Register message and setup ReceiveListener
      ptp.registerListener(PingMessage.class, new MessageReceivedListener<PingMessage>() {
        @Override
        public void messageReceived(PingMessage message, Identifier source) {
          sendPong(message.seq, source);
        }
      });
      
      // Register message and setup ReceiveListener
      ptp.registerListener(PongMessage.class, new MessageReceivedListener<PongMessage>() {
        @Override
        public void messageReceived(PongMessage message, Identifier source) {
          
          // quick and dirty approximation
          long sendTime = pingStartTime + (message.seq - 1) * pingInterval;
          long rtt = System.currentTimeMillis() - sendTime;
          System.out.println("Received pong from " + source + ":"
              + " seq=" + message.seq
              + " time=" + rtt + " ms");
        }
      });
      
      // Output the created hidden service identifier.
      System.out.println("Own identifier: " + ptp.getIdentifier());      

      Identifier pingTarget = getDestinationFromConsolePrompt();
      
      pingStartTime = System.currentTimeMillis();
      int lastPingSeq = 0;
      
      do {
        int seq = lastPingSeq + 1;
        sendPing(seq, pingTarget);
        lastPingSeq = seq;
        
        // Wait until end of interval
        Thread.sleep(pingInterval);
        
      } while (true);

    } catch (IOException e) {
      e.printStackTrace();
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    // Done, exit.
    System.out.println("Exiting client.");
    ptp.exit();
  }
  
  private static Identifier getDestinationFromConsolePrompt() throws IOException {
    
    // Create a reader for the console input.
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    
    // Ask for the destination hidden service identifier.
    System.out.print("Enter destination identifier: ");
    final String destinationAddress = br.readLine();
    return new Identifier(destinationAddress);
  }
}
