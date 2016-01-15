package edu.kit.tm.ptp.examples;

import edu.kit.tm.ptp.Identifier;
import edu.kit.tm.ptp.PTP;
import edu.kit.tm.ptp.ReceiveListener;
import edu.kit.tm.ptp.SendListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicInteger;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;


/**
 * An example application of the PTP API that is a little more interesting.
 *
 * @author Martin Florian
 *
 */
public class FloodingDarknetExample {

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
      client.reuseHiddenService();

      // Print Identifier to a file
      Files.write(
          Paths.get("identifier.txt"),
          (client.getIdentifier().toString() + "\n").getBytes("utf-8"),
          StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

      // Read friends
      List<String> lines = Files.readAllLines(Paths.get("friends.txt"), Charset.forName("UTF-8"));
      for(String line : lines){
        System.out.println(line);
      }
      
      client.exit();

      // Setup ReceiveListener
      client.setReceiveListener(new ReceiveListener() {
        @Override
        public void messageReceived(byte[] data, Identifier source) {
          System.out.println("Received message: " + new String(data) + " from " + source);
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
          switch (state) {
            case INVALID_DESTINATION:
              System.out.println("Destination " + destination + " is invalid");
              break;
            case SUCCESS:
              counter.incrementAndGet();
              break;
            case TIMEOUT:
              System.out.println("Sending of message timed out");
              break;
            default:
              break;
          }
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

      long start = System.currentTimeMillis();
      // Wait until all messages are sent.
      System.out.println("Sleeping.");
      while (counter.get() < sent && System.currentTimeMillis() - start < 3000) {
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
