package dispatch;

import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import api.Message;
import callback.DispatchListener;
import callback.SendListener;
import utility.Constants;


/**
 * A dispatcher for API messages. Keeps a queue of unsent messages per destination and attempts to (re-)send the messages using a thread pool.
 *
 * @author Simeon Andreev
 *
 */
public class MessageDispatcher {

	/** The logger for this class. */
	private final Logger logger = Logger.getLogger(Constants.dispatcherlogger);
	/** The set of message queues for all destinations. */
	private final HashMap<String, Worker> map = new HashMap<String, Worker>();
	/** The set of destinations with empty message queues. */
	private final ConcurrentLinkedQueue<String> removed = new ConcurrentLinkedQueue<String>();
	/** The workers in the thread pool which will handle the message queues. */
	private final Worker workers[];
	/** The listener that will be notified of empty destination queues. */
	private final Worker.Listener doneListener = new Worker.Listener() {

		@Override
		public void done(String destination) { remove(destination); }

	};


	/**
	 * Constructor method.
	 *
	 * @param listener The parameters of this dispatcher.
	 * @param client The raw API client to use when sending messages.
	 * @param manager The TTL manager to notify of established connections.
	 */
	public MessageDispatcher(DispatchListener dispatchListener, int threads) {
		workers = new Worker[threads];
		for (int i = 0; i < workers.length; ++i)
			workers[i] = new Worker(dispatchListener, doneListener);

		logger.log(Level.INFO, "Message dispatcher object created with maximum threads number: " + threads);
	}


	/**
	 * Adds a message to a destination queue and sets a thread to handle the message dispatch.
	 *
	 * @param message The message to add to the queue for sending.
	 * @param timeout The timeout for the sending.
	 * @param listener The listener to notify of sending events.
	 */
	public synchronized void enqueueMessage(Message message, long timeout, SendListener listener) {
		String destination = message.destination.getTorAddress();
		Element element = new Element(message, timeout, listener);

		// Update the destination set by removing destinations with empty queues.
		while (!removed.isEmpty()) {
			String d = removed.poll();
			map.remove(d);
		}

		// Find a worker for the message.
		if (!map.containsKey(destination)) {
			long minimumLoad = Long.MAX_VALUE;
			int index = 0;

			// Find a free worker and assign the new queue.
			for (int i = 0; i < workers.length; ++i) {
				if (!workers[i].running()) {
					index = i;
					break;
				}

				final long load = workers[i].load();

				// If no free worker is available, assign the new message to the worker with the least load.
				if (load < minimumLoad) {
					minimumLoad = load;
					index = i;
				}
			}

			// Add the message to the chosen worker and map the destination to that worker.
			workers[index].addMessage(element);
			map.put(destination, workers[index]);
		} else
			map.get(destination).addMessage(element);

		logger.log(Level.INFO, "Message enqueued: " + message.content + " (" + message.identifier + ")");
	}

	/**
	 * Sends a stop signal to all dispatcher threads.
	 */
	public void stop() {
		logger.log(Level.INFO, "Stopping message dispatcher.");

		for (int i = 0; i < workers.length; ++i)
			workers[i].stop();
	}


	/**
	 * Marks a destination with an empty queue.
	 *
	 * @param destination The destination of the queue that went empty.
	 */
	private void remove(String destination) {
		removed.add(destination);
	}

}
