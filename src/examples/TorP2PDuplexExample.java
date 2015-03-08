package examples;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import api.Identifier;
import api.Message;
import api.TorP2P;
import callback.ReceiveListener;
import callback.SendListener;

/**
 * Duplex connection usage example application for the TorP2P API.
 *
 * @author Simeon Andreev
 *
 */
public class TorP2PDuplexExample {


	/**
	 * Custom listener for the ping-pong sending.
	 *
	 * @author Simeon Andreev
	 *
	 */
	private static class MyListener implements ReceiveListener {

		/** The "name" of this listener. */
		private final String name;
		/** The "opposite" client to which a message should be sent whenever a message is received.  */
		private final TorP2P client;
		/** The maximum number of sends that may be initiated. */
		private final int max;
		/** The timeout (in milliseconds) for each sending. */
		private final int timeout;
		/** An atomic counter for the number of sendings. */
		private final AtomicInteger counter = new AtomicInteger(0);
		/** The sending start time. */
		private long start = 0;
		/** Total RTT time. */
		private long duration = 0;


		/**
		 * Constructor method.
		 *
		 * @param client The "opposite" client for the ping-pong sending.
		 * @aram max The maximum number of sends that may be initiated.
		 * @param timeout The timeout (in milliseconds) for each sending.
		 */
		public MyListener(String name, TorP2P client, int max, int timeout) {
			this.name = name;
			this.client = client;
			this.max = max;
			this.timeout = timeout;
		}


		/**
		 * @see ReceiveListener
		 */
		@Override
		public void receivedMessage(Message message) {
			System.out.println("Listener " + name + " received: " + message.content);
			// No more sending if maximum was reached.
			if (counter.get() > max) return;
			// Measure sending RTT time.
			if (counter.get() > 0) {
				duration += System.currentTimeMillis() - start;
			}
			// Send message back.
			start = System.currentTimeMillis();
			client.sendMessage(message, timeout);
			counter.incrementAndGet();
		}


		/**
		 * Returns the number of sends initiated so far.
		 *
		 * @return The number of sends initiated so far
		 */
		public int sent() {  return counter.get();  }

		/**
		 * Returns the total RTT.
		 *
		 * @return The total RTT.
		 */
		public long duration() { return duration; }

	}


	/**
	 * The main method.
	 *
	 * @param args Program parameters, ignored.
	 * @throws IllegalArgumentException Propagates any IllegalArgumentException thrown by the configuration.
	 * @throws IOException Propagates any IOException received by the API, or thrown by the console read operations.
	 */
	public static void main(String[] args) {
		TorP2P client1 = null;
		TorP2P client2 = null;

		try {
			// Create API wrapper objects.
			client1 = new TorP2P();
			client2 = new TorP2P();

			client1.createHiddenService();

	        // Set the timer start for the hidden service creation measurement.
			long start = System.currentTimeMillis();
			final Identifier identifier1 = client1.getIdentifier();
			final long duration = System.currentTimeMillis() - start;

	       	// Connect to the destination hidden service and port by sending a dummy message.
			System.out.println("Connecting client.");
			final AtomicBoolean connected = new AtomicBoolean(false);
			final long timeout = 120 * 1000;
			final Message m = new Message("Hello.", identifier1);

			client2.sendMessage(m, timeout, new SendListener() {

				@Override
				public void connectionSuccess(Message message) { connected.set(true); }

				@Override
				public void connectionTimeout(Message message) {}

				@Override
				public void sendSuccess(Message message) {}

				@Override
				public void sendFail(Message message) {}

			});

			// Wait for the sending result.
			long waitStart = System.currentTimeMillis();
			while (System.currentTimeMillis() - waitStart <= timeout + (5 * 1000) && !connected.get()) {
				try {
					Thread.sleep(1 * 1000);
				} catch (InterruptedException e) {
					// Sleeping was interrupted. Do nothing.
				}
			}
			if (!connected.get())
				throw new IOException("Could not send greeting message in the given timeout.");
			// Set the timer start for the hidden service availability measurement.
			final long available = System.currentTimeMillis() - start;

			// The maximum number of PING-PONGs.
			final int max = 50;
			// Set ping-pong listeners.
			MyListener listener1 = new MyListener("L1", client1, max, 15000);
			client1.setListener(listener1);
			MyListener listener2 = new MyListener("L2", client2, max, 15000);
			client2.setListener(listener2);


			final int shortTimeout = 15 * 1000;
			// Send the initial message.
			client2.sendMessage(m, shortTimeout);

			// Wait until the maximum number of PING-PONGs (or the timeout) is reached.
			System.out.println("Sleeping.");
			waitStart = System.currentTimeMillis();
			while ((listener1.sent() < max || listener2.sent() < max) && System.currentTimeMillis() - waitStart <= 120 * 1000) {
				try {
					// Sleep.
					Thread.sleep(5 * 1000);
				} catch (InterruptedException e) {
					System.out.println("Main thread interrupted.");
				}
			}

			if (listener1.sent() < max || listener2.sent() < max)
				throw new IOException("Maximum number of ping-pongs not reached!");

			// Show time measurements.
			System.out.println("Displaying running time statistics.");
			System.out.println("\t To create HS         : " + duration + " ms.");
			System.out.println("\t Wait until reachable : " + available + " ms.");
			System.out.println("\t RTT client1          : " + listener1.duration() / listener1.sent() + " ms.");
			System.out.println("\t RTT client2          : " + listener2.duration() / listener2.sent() + " ms.");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}

		// Done, exit.
		System.out.println("Exiting clients.");
		if (client1 != null)
			client1.exit();
		if (client2 != null)
			client2.exit();
	}

}
