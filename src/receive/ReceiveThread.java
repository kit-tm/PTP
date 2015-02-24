package receive;

import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import adapters.ReceiveListenerAdapter;
import api.Identifier;
import api.Message;
import api.MessageHandler;
import api.Packet;
import callback.ConnectionListener;
import callback.ReceiveListener;
import thread.Worker;
import utility.Constants;


/**
 * Worker thread class which receives messages from multiple sockets.
 * Does nothing while no open sockets are available.
 * Checks open sockets for incoming data at a specific interval.
 * Delegates received messages to a listener.
 *
 * @author Simeon Andreev
 *
 */
public class ReceiveThread extends Worker<Origin> {

	/** The set of sockets on which this thread listens. */
	private final Vector<Origin> origins = new Vector<Origin>();
	/** The listener to notify on newly opened connections. */
	private final ConnectionListener connectionListener;
	/** The listener to notify on received messages. */
	private ReceiveListener receiveListener = new ReceiveListenerAdapter();
	/** The poll interval (in milliseconds) at which this thread checks for available socket data. */
	private final int pollInterval;

	/**
	 * Constructor method.
	 *
	 * @param connectionListener The listener to notify on newly opened connections.
	 * @param pollInterval The interval (in milliseconds) at which this thread checks the open sockets for incoming data.
	 */
	public ReceiveThread(ConnectionListener connectionListener, int pollInterval) {
		this.connectionListener = connectionListener;
		this.pollInterval = pollInterval;
		load = 0;
	}


	/**
	 * @see Worker
	 */
	@Override
	public void run() {
		running.set(true);
		while (condition.get()) {
			// Get the open sockets.
			Vector<Origin> open = getSockets();
			boolean read = false;

			// Iterate the open sockets.
			for (Origin origin : open) {
				try {
					// Get the input stream of the current socket and check availability.
					InputStream stream = origin.socket.getInputStream();
					final int available = stream.available();

					if (available == 0) continue;

					read = true;

					// Read the received messages.
					byte[] buffer = new byte[available];
					stream.read(buffer, 0, available);
					String bulk = new String(buffer);

					// Unwrap messages.
					Packet[] packets = MessageHandler.unwrapBulk(bulk);
					for (int i = 0; i < packets.length; ++i) {
						// Set origin if available.
						if (packets[i].flags == Constants.messageoriginflag) {
							origin.identifier = new Identifier(packets[i].message.content);
							connectionListener.ConnectionOpen(origin.identifier, origin.socket);
						// Otherwise notify listener.
						} else
							receiveListener.receivedMessage(new Message(packets[i].message.content, origin.identifier));
					}
				} catch (IOException e) {
					// Socket was closed. Do nothing.
				} catch (NullPointerException e) {
					// Socket was closed. Do nothing.
				}
			}

			// If open sockets remain and no data was available sleep at the given interval.
			if (condition.get() && !read) {
				try {
					Thread.sleep(pollInterval);
				} catch (InterruptedException e) {
					// Sleeping was interrupted. Do nothing.
				}
			}
		}
		running.set(false);
	}

	/**
	 * @see Worker
	 */
	@Override
	public synchronized void enqueue(Origin socket) {
		// Add the socket to the socket queue.
		origins.add(socket);
		++load;

		// Check if the thread is not running, if so wake it.
		if (!running.get()) {
			// Possibly wait for the thread to exit its execution loop completely.
			while (thread.isAlive())
			{
				try {
					thread.join();
				} catch (InterruptedException e) {
					// Got interrupted. Do nothing.
				}
			}
			// Start the thread.
			running.set(true);
			start();
		// Otherwise, notify the thread if it is sleeping.
		} else
			thread.interrupt();
	}

	/**
	 * Sets the listener to notify on received messages.
	 *
	 * @param listener The listener to notify on received messages.
	 */
	public void setListener(ReceiveListener listener) {
		this.receiveListener = listener;
	}


	/**
	 * Removes the closed sockets from the sockets set and returns the open sockets.
	 *
	 * @return The the sockets which are still open.
	 */
	private synchronized Vector<Origin> getSockets() {
		// Get the open sockets from the sockets set.
		Vector<Origin> open = new Vector<Origin>(origins.size());
		for (int i = 0; i < origins.size(); ++i) {
			if (origins.get(i).socket != null && !origins.get(i).socket.isClosed())
				open.add(origins.get(i));
		}

		// Update the sockets set.
		origins.clear();
		origins.setSize(open.size());
		for (int i = 0; i < open.size(); ++i)
			origins.set(i, open.get(i));

		// If no origins remain, stop the thread.
		if (origins.isEmpty()) {
			condition.set(false);
			running.set(false);
		}

		load = open.size();

		return open;
	}

}
