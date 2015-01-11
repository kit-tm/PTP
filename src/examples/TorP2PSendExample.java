package examples;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import utility.DateUtils;
import api.Identifier;
import api.Message;
import api.TorP2P;
import callback.SendListener;
import callback.SendListenerAdapter;


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
				public void connectionTimeout(Message message) { counter.incrementAndGet(); }

			};

			System.out.println("Enter # messages to send:");
			String number = br.readLine();
			final int max = Integer.valueOf(number);
			Random random = new Random();
			Random delay = new Random();

			final DateUtils.ExactCalendar c = DateUtils.getAtomicTime();

			// Send the messages.
			for (int i = 0; i < max; ++i) {
				String content = i + " msg " + random.nextInt(1000);
				String timestamped = (c.calendar.getTimeInMillis() + System.currentTimeMillis() - c.current) + " " + content;
				Message message = new Message(timestamped, destination);
				client.SendMessage(message, timeout, listener);
				try {
					Thread.sleep((1 + delay.nextInt(5)) * 1000);
				} catch (InterruptedException e) {
					// Do nothing.
				}
			}

			// Wait until all messages are sent.
			System.out.println("Sleeping.");
			while (counter.get() < max) {
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
		if (client != null) client.Exit();
	}

}
