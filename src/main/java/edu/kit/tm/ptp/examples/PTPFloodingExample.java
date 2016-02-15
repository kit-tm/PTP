package edu.kit.tm.ptp.examples;

import edu.kit.tm.ptp.Identifier;
import edu.kit.tm.ptp.MessageReceivedListener;
import edu.kit.tm.ptp.PTP;
import edu.kit.tm.ptp.SendListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Floods entered messages in a network of befriended peers.
 *
 * @author Martin Florian
 *
 */
public class PTPFloodingExample {

  static PTP ptp;
  
  /**
   * Message format.
   */
  static class FloodingMessage {
    public String content;
    public long timestamp;
    
    // no-arg constructor required for PTP 
    public FloodingMessage() {
      content = null;
      timestamp = -1;
    }
    
    public FloodingMessage(String message) {
      content = message;
      timestamp = System.currentTimeMillis();
    }
    
    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj == null || obj.getClass() != this.getClass()) {
        return false;
      }
      FloodingMessage fm = (FloodingMessage) obj;
      return (this.content.equals(fm.content) && (this.timestamp == fm.timestamp));
    }
    
    @Override
    public int hashCode() {
      // could probably be done better
      String tmp = content + String.valueOf(timestamp);
      return tmp.hashCode();
    }
  }
    
  /**
   * Starts the example.
   * 
   * @param args Not used.
   */
  public static void main(String[] args) {
    
    // Create a PTP object.
    final PTP ptp = new PTP();

    try {
      // Initialize
      System.out.print("Initializing PTP...");
      ptp.init();
      System.out.println(" done.");

      // Setup own identifier, reusing existing one if possible
      ptp.reuseHiddenService();

      // Print own identifier to a file
      Files.write(
          Paths.get("identifier.txt"),
          (ptp.getIdentifier().toString() + "\n").getBytes("utf-8"),
          StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

      // Read friends
      final List<Identifier> friends = new ArrayList<Identifier>();
      try {
        for (String line : Files.readAllLines(Paths.get("friends.txt"), Charset.forName("UTF-8"))) {

          Identifier friend = new Identifier(line);
          
          if (!friend.isValid() || friend.equals(ptp.getIdentifier())) {
            System.out.println("Skipping invalid friend entry: " + friend);
            continue;
          }
          friends.add(new Identifier(line));
        }
      } catch (IOException e) {
        System.out.println("Error reading \"friends.txt\", continuing without friends :(");
      }
      
      final Set<FloodingMessage> seenMessages = new HashSet<FloodingMessage>();

      // Register message and setup ReceiveListener
      ptp.registerMessage(FloodingMessage.class, new MessageReceivedListener<FloodingMessage>() {
        
        @Override
        public void messageReceived(FloodingMessage message, Identifier source) {
          
          // to avoid loops...
          synchronized (seenMessages) {
            if (seenMessages.contains(message)) {
              //System.out.println("Received duplicate message from " + source);
              return;
            }
            seenMessages.add(message);
          }
          
          System.out.println(
              "Received message: " + message.content + " from " + source + "; "
              + "The message is " + (System.currentTimeMillis() - message.timestamp) + "ms old");
          
          // forward to all friends
          synchronized (friends) {
            for (Identifier friend : friends) {
              
              if (friend.equals(source)) {
                continue;
              }
              
              System.out.println("Forwarding to " + friend);
              ptp.sendMessage(message, friend);
            }
            
            // add new friends
            if (!friends.contains(source)) {
              System.out.println("Added new friend: " + source);
              friends.add(source);
            }
          }
        }
      });

      // Create a reader for the console input.
      BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

      ptp.setSendListener(new SendListener() {
        @Override
        public void messageSent(long id, Identifier destination, State state) {
          switch (state) {
            case INVALID_DESTINATION:
              System.out.println("Destination " + destination + " is invalid");
              break;
            case TIMEOUT:
              System.out.println("Sending of message timed out");
              break;
            default:
              break;
          }
        }
      });
      
      // print own identifier
      System.out.println("Own identifier: " + ptp.getIdentifier());
      
      // main loop
      while (true) {
        System.out.println("Enter message to send (or exit to stop):");
        String content = br.readLine();
        if (content.equals("exit")) {
          break;
        }
        synchronized (friends) {
          for (Identifier friend : friends) {
            
            FloodingMessage message = new FloodingMessage(content);
            
            synchronized (seenMessages) {
              seenMessages.add(message);
            }
            
            System.out.println("Sending to " + friend);
            ptp.sendMessage(message, friend);
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    // Done, exit.
    System.out.println("Exiting client.");
    
    if (ptp != null) {
      ptp.exit();
    }
  }
}
