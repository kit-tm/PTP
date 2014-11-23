package examples;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import p2p.Client;
import p2p.Configuration;
import p2p.Constants;
import p2p.Listener;
import thread.TorManager;


/**
 * An test application of the raw API, sends a message to the local identifier.
 *
 * @author Simeon Andreev
 *
 */
public class RawAPISendSelfExample {


	public static void main(String[] args) throws IllegalArgumentException, IOException {
		final AtomicBoolean received = new AtomicBoolean(false);
		final String message = "the Message";

		TorManager manager = new TorManager();
		// Start the TorManagers.
		manager.start();

		// Wait (no more than 3 minutes) until the two TorManagers are done with their respective Tor bootstrapping.
		while ((!manager.ready())) {
			try {
				Thread.sleep(1 * 1000);
			} catch (InterruptedException e) {
				// Sleeping was interrupted. Do nothing.
			}
		}

		final Configuration configuration = new Configuration(Constants.configfile, "./config", manager.controlport(), manager.socksport());
		Client client = new Client(configuration);
		client.listener(new Listener() {

			@Override
			public void receive(byte[] bytes) {
				System.out.println("Received message: " + new String(bytes));
				if (new String(bytes).equals(message))
					System.out.println("Received message matches sent message.");
				else
					System.out.println("Received message does not match sent message.");
				received.set(true);
			}

		});
		final String identifier = client.identifier(true);

		System.out.println("Connecting to the indentifier.");
		Client.ConnectResponse connect = Client.ConnectResponse.TIMEOUT;
		while (connect == Client.ConnectResponse.TIMEOUT || connect == Client.ConnectResponse.FAIL) {
			try {
				connect = client.connect(identifier);
				Thread.sleep(5 * 1000);
			} catch (InterruptedException e) {
				System.out.println("Main thread interrupted.");
			}
		}
		System.out.println("Sending message.");
		client.send(identifier, message);

		System.out.println("Sleeping.");
		while (!received.get()) {
			try {
				Thread.sleep(5 * 1000);
			} catch (InterruptedException e) {
				System.out.println("Main thread interrupted.");
			}
		}

		System.out.println("Disconnecting.");
		client.disconnect(identifier);
		System.out.println("Exiting.");
		client.exit();
		manager.stop();
	}

}
