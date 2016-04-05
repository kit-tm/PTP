package edu.kit.tm.ptp.examples;

import edu.kit.tm.ptp.Identifier;
import edu.kit.tm.ptp.PTP;
import edu.kit.tm.ptp.ReceiveListener;
import edu.kit.tm.ptp.SendListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

/**
 * An example application of the PTP API, sends messages to a chosen destination.
 *
 * @author Simeon Andreev
 *
 */
public class PTPSendExample {
  
  private static final String charset = "UTF-8";

  /**
   * Starts the example.
   * 
   * @param args Not used.
   */
  public static void main(String[] args) {

    // Create a PTP object.
    PTP ptp = new PTP();

    try {
      // Initialize
      System.out.print("Initializing PTP...");
      ptp.init();
      System.out.println(" done.");

      // Setup Identifier
      ptp.createHiddenService();
      System.out.println("Own identifier: " + ptp.getIdentifier().toString());

      // Setup ReceiveListener
      ptp.setReceiveListener(new ReceiveListener() {
        @Override
        public void messageReceived(byte[] data, Identifier source) {
          try {
            System.out.println(
                "Received message: " + new String(data, charset) + " from " + source);
          } catch (UnsupportedEncodingException e) {
            System.err.println("Failed to encode String using charset " + charset);
          }
        }
      });

      // Create a reader for the console input
      BufferedReader br = new BufferedReader(new InputStreamReader(System.in, charset));

      // Ask for the destination hidden service identifier
      System.out.print("Enter destination identifier: ");
      final String destinationAddress = br.readLine();
      final Identifier destination = new Identifier(destinationAddress);
      final Thread exampleThread = Thread.currentThread();

      ptp.setSendListener(new SendListener() {

        @Override
        public void messageSent(long id, Identifier destination, State state) {
          switch (state) {
            case INVALID_DESTINATION:
              System.out.println("Destination " + destination + " is invalid");
              exampleThread.interrupt();
              break;
            case TIMEOUT:
              System.out.println("Sending of message timed out");
              break;
            default:
              break;
          }
        }
      });

      while (!Thread.interrupted()) {
        System.out.println("Enter message to send (or exit to stop):");
        String content = br.readLine();
        if (content.equals("exit")) {
          break;
        }
        ptp.sendMessage(content.getBytes(charset), destination);
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    }

    // Done, exit.
    System.out.println("Exiting client.");
    ptp.exit();
  }
}
