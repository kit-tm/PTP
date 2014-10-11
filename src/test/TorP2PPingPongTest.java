package test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicInteger;

import p2p.Configuration;
import p2p.Listener;
import p2p.TorP2P;


/**
 * Ping-Pong test application for the TorP2P API.
 *
 * @author Simeon Andreev
 *
 */
public class TorP2PPingPongTest {

	private static class MyListener implements Listener {

		private final TorP2P client;
		private final String identifier;
		private final int port;
		private final int max;

		private final AtomicInteger counter = new AtomicInteger();

		private String message = null;
		private long start = 0;
		private long duration = 0;
		private int sent = 0;


		public MyListener(TorP2P client, String identifier, int port, int max) {
			this.client = client;
			this.identifier = identifier;
			this.port = port;
			this.max = max;
		}


		public void send(String message, boolean response) {
			if (counter.get() < max) {
				client.SendMessage(message + (response ? " " + counter.get() : ""), identifier, port, 1 * 1000);
				start = System.currentTimeMillis();
				++sent;
			}
			counter.incrementAndGet();
		}

		public int count() { return counter.get(); }

		public long duration() { return duration; }

		public int sent() { return sent; }

		@Override
		public void receive(byte[] bytes) {
			final String received = new String(bytes);
			System.out.println("Client received message: " + received);
			if (message == null) message = received;
			else duration += System.currentTimeMillis() - start;
			send(message, true);
		}

	}


	public static void main(String[] args) throws IllegalArgumentException, IOException {
		try {
			final TorP2P client = new TorP2P();
	        final Configuration configuration = new Configuration("config/p2p.ini");

			long start = System.currentTimeMillis();
			final String identifier = client.GetIdentifier();
			final long duration = System.currentTimeMillis() - start;

			System.out.println("Created hidden service.");
			System.out.println("Identifier : " + identifier);
			System.out.println("Port       : " + configuration.getHiddenServicePort());

			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
	        System.out.print("Enter hidden service identifier:");
	        final String destination = br.readLine();
	        System.out.print("Enter hidden service port:");
	       	final int port = Integer.valueOf(br.readLine());

			System.out.println("Connecting client.");
			TorP2P.SendResponse connect = TorP2P.SendResponse.TIMEOUT;
			start = System.currentTimeMillis();
			while (connect == TorP2P.SendResponse.TIMEOUT || connect == TorP2P.SendResponse.FAIL) {
				try {
					connect = client.SendMessage("", destination, port, 1 * 1000);
					Thread.sleep(5 * 1000);
				} catch (InterruptedException e) {
					System.out.println("Main thread interrupted.");
				}
			}
			final long available = System.currentTimeMillis() - start;

			final int max = 50;
			final MyListener listener = new MyListener(client, destination, port, max);
			client.SetListener(listener);


			System.out.println("Connected.");
			while (true) {
				System.out.println("Send message? (yes/no)");
				String line = br.readLine();

				if (line.equals("yes")) {
					System.out.println("Enter message to send:");
					String message = br.readLine();
					System.out.println("Sending first message.");
					listener.send(message, false);
					break;
				} else if (line.equals("no"))
					break;
			}

			System.out.println("Sleeping.");
			while (listener.count() < max) {
				try {
					Thread.sleep(5 * 1000);
				} catch (InterruptedException e) {
					System.out.println("Main thread interrupted.");
				}
			}

			System.out.println("Displaying running time statistics.");
			System.out.println("\t To create HS         : " + duration + " ms.");
			System.out.println("\t Wait until reachable : " + available + " ms.");
			if (listener.sent() > 0)
				System.out.println("\t RTT                  : " + listener.duration() / listener.sent() + " ms.");

			System.out.println("Exiting client.");
			client.Exit();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
