package examples;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

import p2p.Client;
import p2p.Configuration;
import p2p.Constants;
import p2p.Listener;


/**
 * Ping-Pong test application for the raw API.
 *
 * @author Simeon Andreev
 *
 */
public class RawAPIPingPongTest {

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
				client.send(identifier, message + (response ? " " + counter.get() : ""));
				start = System.currentTimeMillis();
				++sent;
			}
			counter.incrementAndGet();
		}

		public void receive() {
			duration += System.currentTimeMillis() - start;
		};

	}


	public static void main(String[] args) throws IllegalArgumentException, IOException {
		Client client = null;

		try {
			final Holder holder = new Holder();
			holder.max = 50;

	        final Configuration configuration = new Configuration(Constants.configfile, Paths.get("config"), 9051, 9050);
			client = new Client(configuration);
			client.listener(new Listener() {

				@Override
				public void receive(byte[] bytes) {
					final String message = new String(bytes);
					System.out.println("Client received message: " + message);
					if (holder.message == null) holder.message = message;
					else holder.receive();
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
					connect = client.connect(holder.identifier);
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
				} else if (line.equals("no"))
					break;
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
			if (holder.sent > 0)
				System.out.println("\t RTT                  : " + holder.duration / holder.sent + " ms.");
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Exiting client.");
		if (client != null)
			client.exit();
	}

}
