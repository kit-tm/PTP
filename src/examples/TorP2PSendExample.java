package examples;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicInteger;

import utility.DateUtils;
import adapters.SendListenerAdapter;
import api.Identifier;
import api.Message;
import api.TorP2P;
import callback.SendListener;


/**
 * An example application of the TorP2P API, sends messages to a chosen destination.
 *
 * @author Simeon Andreev
 *
 */
public class TorP2PSendExample {


	public static void main(String[] args) {
		TorP2P client = null;

		try {
			// Create an API wrapper object.
			System.out.println("Initializing API.");
			client = new TorP2P();

			// Create a reader for the console input.
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			// Ask for the destination hidden service identifier.
	        System.out.print("Enter hidden service identifier:");
	        final String destinationAddress = br.readLine();
	        final Identifier destination = new Identifier(destinationAddress);
			final long timeout = 60 * 1000;

			final AtomicInteger counter = new AtomicInteger(0);
			SendListener listener = new SendListenerAdapter() {

				@Override
				public void sendSuccess(Message message) { counter.incrementAndGet(); }

				@Override
				public void sendFail(Message message, FailState state) { counter.incrementAndGet(); }

			};

			final DateUtils.Time current = DateUtils.getAtomicTime();
			int sent = 0;

			while (true) {
				System.out.println("Enter message to send (or exit to stop):");
				String content = br.readLine();
				if (content.equals("exit")) break;
				String timestamped = (current.internet  + System.currentTimeMillis() - current.local) + " " + content;
				Message message = new Message(timestamped, destination);
				client.sendMessage(message, timeout, listener);
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
		if (client != null) client.exit();
	}

}
