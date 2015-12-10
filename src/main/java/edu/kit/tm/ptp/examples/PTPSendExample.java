package edu.kit.tm.ptp.examples;

import edu.kit.tm.ptp.Identifier;
import edu.kit.tm.ptp.PTP;
import edu.kit.tm.ptp.ReceiveListener;
import edu.kit.tm.ptp.SendListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An example application of the PTP API, sends messages to a chosen destination.
 *
 * @author Simeon Andreev
 *
 */
public class PTPSendExample {

  /**
   * Starts the example.
   * 
   * @param args Not used.
   */
  public static void main(String[] args) {
    PTP client = null;

    try {
      // Create an API wrapper object.
      System.out.println("Initializing API.");
      client = new PTP();

      // Setup Identifier
      client.createHiddenService();
      System.out.println("Own identifier: " + client.getIdentifier().toString());

      // Setup ReceiveListener
      client.setReceiveListener(new ReceiveListener() {
        @Override
        public void messageReceived(byte[] data, Identifier source) {
          System.out.println("Received message: " + new String(data));
        }
      });

      // Create a reader for the console input.
      BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
      // Ask for the destination hidden service identifier.
      System.out.print("Enter destination identifier: ");
      final String destinationAddress = br.readLine();
      final Identifier destination = new Identifier(destinationAddress);
      final long timeout = 60 * 1000;

      final AtomicInteger counter = new AtomicInteger(0);
      client.setSendListener(new SendListener() {
        
        @Override
        public void messageSent(long id, Identifier destination, State state) {
          counter.incrementAndGet();
        }
      });

      int sent = 0;

      while (true) {
        System.out.println("Enter message to send (or exit to stop):");
        String content = br.readLine();
        if (content.equals("exit")) {
          break;
        }
        client.sendMessage(content.getBytes(), destination, timeout);
        ++sent;
      }

      // Wait until all messages are sent.
      System.out.println("Sleeping.");
      while (counter.get() < sent) {
        try {
          // Sleep.
          Thread.sleep(5 * 1000);
        } catch (InterruptedException e) {
          System.out.println("Main thread interrupted.");
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    }

    // Done, exit.
    System.out.println("Exiting client.");
    if (client != null) {
      client.exit();
    }
  }

}
