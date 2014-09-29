package test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicInteger;

import p2p.Client;
import p2p.Configuration;
import p2p.Listener;


public class PingPongTest {

	// TODO: this needs multiple hidden services support, for which the API needs to change
	/*private static class PingPong {

		public Client client1 = null;
		public String identifier1 = null;

		public Client client2 = null;
		public String identifier2 = null;


		public PingPong() {}


		public void pongClient1(String message) {
			if (client2 != null && identifier1 != null)
				client2.send(identifier1, message);
		}


		public void pongClient2(String message) {
			if (client1 != null && identifier2 != null)
				client1.send(identifier2, message);
		}

	}


	public static void main(String[] args) throws IllegalArgumentException, IOException {
		final String base = "the Message";
		final PingPong pingpong = new PingPong();
		final int max = 50;
		final AtomicInteger received = new AtomicInteger(0);

		Configuration configuration1 = new Configuration("config/p2p.ini");
		final Client client1 = new Client(configuration1, new Listener() {

			@Override
			public void receive(byte[] bytes) {
				final String message = new String(bytes);
				System.out.println("Client 1 received message: " + message);
				pingpong.pongClient2(message.substring(0, base.length()) + " " + received.get());
				received.incrementAndGet();
			}

		});
		final String identifier1 = client1.identifier(true);
		pingpong.client1 = client1;
		pingpong.identifier1 = identifier1;

		Configuration configuration2 = new Configuration("config/p2p-aux.ini");
		final Client client2 = new Client(configuration2, new Listener() {

			@Override
			public void receive(byte[] bytes) {
				final String message = new String(bytes);
				System.out.println("Client 2 received message: " + message);
				pingpong.pongClient1(message.substring(0, base.length()) + " " + received.get());
				received.incrementAndGet();
			}

		});
		final String identifier2 = client2.identifier(true);
		pingpong.client2 = client2;
		pingpong.identifier2 = identifier2;

		System.out.println("Connecting client 1 to client 2.");
		Client.ConnectResponse connect1 = Client.ConnectResponse.TIMEOUT;
		while (connect1 == Client.ConnectResponse.TIMEOUT || connect1 == Client.ConnectResponse.FAIL) {
			try {
				connect1 = client1.connect(identifier2, configuration2.getHiddenServicePort());
				Thread.sleep(5 * 1000);
			} catch (InterruptedException e) {
				System.out.println("Main thread interrupted.");
			}
		}

		System.out.println("Connecting client 2 to client 1.");
		Client.ConnectResponse connect2 = Client.ConnectResponse.TIMEOUT;
		while (connect2 == Client.ConnectResponse.TIMEOUT || connect2 == Client.ConnectResponse.FAIL) {
			try {
				connect2 = client2.connect(identifier1, configuration1.getHiddenServicePort());
				Thread.sleep(5 * 1000);
			} catch (InterruptedException e) {
				System.out.println("Main thread interrupted.");
			}
		}


		System.out.println("Sending first message.");
		client1.send(identifier2, base);

		System.out.println("Sleeping.");
		while (received.get() < max) {
			try {
				Thread.sleep(5 * 1000);
			} catch (InterruptedException e) {
				System.out.println("Main thread interrupted.");
			}
		}

		System.out.println("Disconnecting client 1.");
		client1.disconnect(identifier2);
		System.out.println("Disconnecting client 2.");
		client2.disconnect(identifier1);
		System.out.println("Exiting client 1.");
		client1.exit();
		System.out.println("Exiting client 2.");
		client2.exit();
	}*/


	private static class Holder {

		public Client client;
		public String identifier;
		public int port;
		public int max;

		public AtomicInteger counter = new AtomicInteger();
		public String message = null;


		public Holder() {}


		public void send() {
			if (counter.get() < max)
				client.send(identifier, message + " " + counter.get());
			counter.incrementAndGet();
		}

	}


	public static void main(String[] args) throws IllegalArgumentException, IOException {
		Client client = null;

		try {
			final Holder holder = new Holder();
			holder.max = 50;

	        final Configuration configuration = new Configuration("config/p2p.ini");
			client = new Client(configuration, new Listener() {

				@Override
				public void receive(byte[] bytes) {
					final String message = new String(bytes);
					System.out.println("Client received message: " + message);
					if (holder.message == null) holder.message = message;
					holder.send();
				}

			});
			final String identifier = client.identifier(true);

			System.out.println("Created hidden service.");
			System.out.println("Identifier : " + identifier);
			System.out.println("Port       : " + configuration.getHiddenServicePort());

			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
	        System.out.print("Enter hidden service identifier:");
	        holder.identifier = br.readLine();
	        System.out.print("Enter hidden service port:");
	        holder.port = Integer.valueOf(br.readLine());
	        holder.client = client;

			System.out.println("Connecting client.");
			Client.ConnectResponse connect = Client.ConnectResponse.TIMEOUT;
			while (connect == Client.ConnectResponse.TIMEOUT || connect == Client.ConnectResponse.FAIL) {
				try {
					connect = client.connect(holder.identifier, holder.port);
					Thread.sleep(5 * 1000);
				} catch (InterruptedException e) {
					System.out.println("Main thread interrupted.");
				}
			}

			System.out.println("Connected.");
			while (true) {
				System.out.println("Send message? (yes/no)");
				String line = br.readLine();

				if (line.equals("yes")) {
					System.out.println("Enter message to send:");
					holder.message = br.readLine();
					System.out.println("Sending first message.");
					holder.send();
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
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Exiting client.");
		if (client != null)
			client.exit();
	}

}
