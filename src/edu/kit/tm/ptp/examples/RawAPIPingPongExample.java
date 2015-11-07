package edu.kit.tm.ptp.examples;

import edu.kit.tm.ptp.Message;
import edu.kit.tm.ptp.ReceiveListener;
import edu.kit.tm.ptp.raw.Client;
import edu.kit.tm.ptp.raw.Configuration;
import edu.kit.tm.ptp.raw.MessageHandler;
import edu.kit.tm.ptp.raw.TorManager;
import edu.kit.tm.ptp.utility.Constants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Ping-Pong example application for the raw API.
 *
 * @author Simeon Andreev
 *
 */
public class RawAPIPingPongExample {

  private static class Holder {

    public Client client;
    public String identifier;
    public int max;

    public AtomicInteger counter = new AtomicInteger();
    public String message = null;

    public long start;
    public long duration = 0;
    public int sent = 0;


    public Holder() {}


    public void send(boolean response) {
      if (counter.get() < max) {
        client.send(identifier, MessageHandler.wrapRaw(
            message + (response ? " " + counter.get() : ""), Constants.messagestandardflag));
        start = System.currentTimeMillis();
        ++sent;
      }
      counter.incrementAndGet();
    }

    public void receive() {
      duration += System.currentTimeMillis() - start;
    }

  }


  public static void main(String[] args) throws IllegalArgumentException, IOException {
    Client client = null;
    TorManager manager = null;

    try {
      final Holder holder = new Holder();
      holder.max = 50;

      manager = new TorManager();
      // Start the TorManagers.
      manager.start();

      // Wait (no more than 3 minutes) until the two TorManagers are done with their respective Tor
      // bootstrapping.
      while ((!manager.ready())) {
        try {
          Thread.sleep(1 * 1000);
        } catch (InterruptedException e) {
          // Sleeping was interrupted. Do nothing.
        }
      }

      final Configuration configuration = new Configuration(Constants.configfile);
      configuration.setTorConfiguration("./config", manager.controlport(), manager.socksport());
      client = new Client(configuration);
      client.listener(new ReceiveListener() {

        @Override
        public void receivedMessage(Message message) {
          System.out.println("Client received message: " + message.content);
          if (holder.message == null) {
            holder.message = message.content;
          } else {
            holder.receive();
          }
          holder.send(true);
        }

      });
      long start = System.currentTimeMillis();
      final String identifier = client.identifier(true);
      final long duration = System.currentTimeMillis() - start;

      System.out.println("Created hidden service.");
      System.out.println("Identifier : " + identifier);
      System.out.println("Port       : " + configuration.getHiddenServicePort());

      BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
      System.out.print("Enter hidden service identifier:");
      holder.identifier = br.readLine();
      holder.client = client;

      System.out.println("Connecting client.");
      Client.ConnectResponse connect = Client.ConnectResponse.TIMEOUT;
      start = System.currentTimeMillis();
      while (connect == Client.ConnectResponse.TIMEOUT || connect == Client.ConnectResponse.FAIL) {
        try {
          connect = client.connect(holder.identifier, configuration.getSocketTimeout());
          Thread.sleep(5 * 1000);
        } catch (InterruptedException e) {
          System.out.println("Main thread interrupted.");
        }
      }
      final long available = System.currentTimeMillis() - start;

      System.out.println("Connected.");
      while (true) {
        System.out.println("Send message? (yes/no)");
        String line = br.readLine();

        if (line.equals("yes")) {
          System.out.println("Enter message to send:");
          holder.message = br.readLine();
          System.out.println("Sending first message.");
          holder.send(false);
          break;
        } else if (line.equals("no")) {
          break;
        }
      }

      System.out.println("Sleeping.");
      while (holder.counter.get() < holder.max) {
        try {
          Thread.sleep(5 * 1000);
        } catch (InterruptedException e) {
          System.out.println("Main thread interrupted.");
        }
      }

      System.out.println("Displaying running time statistics.");
      System.out.println("\t To create HS         : " + duration + " ms.");
      System.out.println("\t Wait until reachable : " + available + " ms.");
      if (holder.sent > 0) {
        System.out.println("\t RTT                  : " + holder.duration / holder.sent + " ms.");
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    System.out.println("Exiting client.");
    if (client != null) {
      client.exit(true);
    }
    if (manager != null) {
      manager.stop();
    }
  }

}
