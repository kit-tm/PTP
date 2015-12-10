package edu.kit.tm.ptp.examples;

import edu.kit.tm.ptp.Identifier;
import edu.kit.tm.ptp.PTP;
import edu.kit.tm.ptp.ReceiveListener;
import edu.kit.tm.ptp.SendListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Ping-Pong example application for the PTP API.
 *
 * @author Simeon Andreev
 *
 */
public class PTPPingPongExample {


  /**
   * The custom listener. Will be notified when a message is received.
   *
   * @author Simeon Andreev
   *
   */
  private static class MyListener implements ReceiveListener {

    /** Handle of the client, needed to PONG upon PING. */
    private final PTP client;
    /** The destination hidden service identifier, where PONG should be sent. */
    private final Identifier identifier;
    /** The maximum number of PING-PONGs. */
    private final int max;

    /**
     * Counter for the number of PING-PONGs. Atomic, as it will be accessed from the main() code to
     * detect the end of the PING-PONG.
     */
    private final AtomicInteger counter = new AtomicInteger();

    /** The message that will act as PONG, set upon the first receive. */
    private String message = null;
    /** Start time, set when a message is sent. Used to measure RTT. */
    private long start = 0;
    /** Total RTT. */
    private long duration = 0;
    /** Total PONGs sent. */
    private int sent = 0;


    /**
     * Constructor method.
     *
     * @param client The client used to send PONGs.
     * @param identifier The destination hidden service identifier for the PONGs.
     * @param port The destination port for the PONGs.
     * @param max The maximum number of PING-PONGs.
     */
    public MyListener(PTP client, Identifier identifier, int max) {
      this.client = client;
      this.identifier = identifier;
      this.max = max;
    }


    /**
     * Sends a message to the destination hidden service identifier and port.
     *
     * @param message The message to send.
     * @param response Switch defining whether this is a PONG (true) or an initial message (false).
     */
    public void send(String message, boolean response) {
      // Check if the maximum PING-PONGs was reached.
      if (counter.get() < max) {
        // Set the timer start.
        start = System.currentTimeMillis();
        // Send the message.
        final String m = message + (response ? " " + counter.get() : "");
        client.sendMessage(m, identifier, 1 * 1000);
        ++sent;
      }
      // Another PING-PONG initiated, increment counter.
      counter.incrementAndGet();
    }

    /**
     * Returns the number of PING-PONGs so far. Thread-safe.
     *
     * @return The number of PING-PONGs.
     */
    public int count() {
      return counter.get();
    }

    /**
     * Returns the total RTT so far.
     *
     * @return The total RTT.
     */
    public long duration() {
      return duration;
    }

    /**
     * The number of messages sent so far.
     *
     * @return The number of messages.
     */
    public int sent() {
      return sent;
    }

    @Override
    public void messageReceived(byte[] data, Identifier source) {
      System.out.println("Client received message: " + new String(data));
      if (message == null) {
        message = new String(data);
      } else {
        duration += System.currentTimeMillis() - start;
      }
      send(message, true);
    }

  }


  /**
   * The main method.
   *
   * @param args Program parameters, ignored.
   */
  public static void main(String[] args) {
    PTP client = null;

    try {
      // Create an API wrapper object.
      client = new PTP();

      client.createHiddenService();

      // Set the timer start for the hidden service creation measurement.
      long start = System.currentTimeMillis();
      final Identifier identifier = client.getIdentifier();
      final long duration = System.currentTimeMillis() - start;

      // Output the created hidden service identifier.
      System.out.println("Created hidden service.");
      System.out.println("Identifier : " + identifier);

      // Create a reader for the console input.
      BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
      // Ask for the destination hidden service identifier.
      System.out.print("Enter hidden service identifier:");
      final String destinationAddress = br.readLine();
      final Identifier destination = new Identifier(destinationAddress);

      // Connect to the destination hidden service and port by sending a dummy message.
      System.out.println("Connecting client.");
      final AtomicBoolean connected = new AtomicBoolean(false);
      final long timeout = 120 * 1000;
      final String m = "";
      client.setSendListener(new SendListener() {

        @Override
        public void messageSent(long id, Identifier destination, State state) {
          if (state == State.SUCCESS) {
            connected.set(true);
          }
        }

      });
      client.sendMessage(m, destination, timeout);
      // Wait for the sending result.
      long waitStart = System.currentTimeMillis();
      while (System.currentTimeMillis() - waitStart <= timeout + (5 * 1000) && !connected.get()) {
        try {
          Thread.sleep(1 * 1000);
        } catch (InterruptedException e) {
          // Sleeping was interrupted. Do nothing.
        }
      }
      if (!connected.get()) {
        throw new IOException("Could not send greeting message in the given timeout.");
      }

      // Wait some extra time until the greeting message is propagated to the current listener.
      waitStart = System.currentTimeMillis();
      while (System.currentTimeMillis() - waitStart <= 2 * 1000) {
        try {
          Thread.sleep(1 * 1000);
        } catch (InterruptedException e) {
          // Sleeping was interrupted. Do nothing.
        }
      }

      // Set the timer start for the hidden service availability measurement.
      final long available = System.currentTimeMillis() - start;

      // The maximum number of PING-PONGs.
      final int max = 50;
      // Create the listener and set it.
      final MyListener listener = new MyListener(client, destination, max);
      client.setReceiveListener(listener);

      // Connected, ask if a message should be sent.
      System.out.println("Connected.");
      while (true) {
        // Ask the user.
        System.out.println("Send message? (yes/no)");
        // Read the user input.
        String line = br.readLine();

        // If the input is "yes", ask for a message to send.
        if (line.equals("yes")) {
          System.out.println("Enter message to send:");
          String message = br.readLine();
          System.out.println("Sending first message.");
          // Send the initial message.
          listener.send(message, false);
          break;
          // If the input is "no" proceed without sending a message.
        } else if (line.equals("no")) {
          break;
          // Only accept "yes" or "no".
        }
      }

      // Wait until the maximum number of PING-PONGs is reached.
      System.out.println("Sleeping.");
      while (listener.count() < max) {
        try {
          // Sleep.
          Thread.sleep(5 * 1000);
        } catch (InterruptedException e) {
          System.out.println("Main thread interrupted.");
        }
      }

      // Show time measurements.
      System.out.println("Displaying running time statistics.");
      System.out.println("\t To create HS         : " + duration + " ms.");
      System.out.println("\t Wait until reachable : " + available + " ms.");
      if (listener.sent() > 0) {
        System.out
            .println("\t RTT                  : " + listener.duration() / listener.sent() + " ms.");
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
