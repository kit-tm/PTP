package dispatch;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import api.Message;
import callback.DispatchListener;
import callback.SendListener;
import thread.Suspendable;
import utility.Constants;


/**
 * A dispatcher for API messages. Keeps a queue of unsent messages per destination and attempts to (re-)send the messages using a thread pool.
 *
 * @author Simeon Andreev
 *
 */
public class MessageDispatcher {

	/** The logger for this class. */
	protected final Logger logger = Logger.getLogger(Constants.dispatcherlogger);
	/** The set of message queues for all destinations. */
	private final HashMap<String, ConcurrentLinkedQueue<Element>> queues = new HashMap<String, ConcurrentLinkedQueue<Element>>();
	/** A lock to synchronize access to the message queue set. */
	private final Lock lock = new ReentrantLock();
	private final Worker workers[];


	/**
	 * Constructor method.
	 *
	 * @param listener The parameters of this dispatcher.
	 * @param client The raw API client to use when sending messages.
	 * @param manager The TTL manager to notify of established connections.
	 */
	public MessageDispatcher(DispatchListener listener, int threads) {
		workers = new Worker[threads];
		for (int i = 0; i < workers.length; ++i)
			workers[i] = new Worker(listener);

		logger.log(Level.INFO, "Message dispatcher object created with maximum threads number: " + threads);
	}


	// TODO: inherit from Suspendable,


	/**
	 * Adds a message to the front of the queue and wakes the dispatcher, so that the message can be sent.
	 *
	 * @param message The message to add to the queue for sending.
	 * @param timeout The timeout for the sending.
	 * @param listener The listener to notify of sending events.
	 */
	public synchronized void enqueueMessage(Message message, long timeout, SendListener listener) {
		final String destination = message.destination.getTorAddress();
		final Element element = new Element(message, timeout, listener);

		// Lock access to the message queues set.
		lock.lock();

		if (!queues.containsKey(destination)) {
			ConcurrentLinkedQueue<Element> queue = new ConcurrentLinkedQueue<Element>();
			queue.add(element);
			queues.put(destination, queue);

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

			// Assign the message to the chosen worker.
			workers[index].add(queue);
			workers[index].start();
		} else
			queues.get(destination).add(element);

		// Unlock access to the message queues set.
		lock.unlock();

		logger.log(Level.INFO, "Message enqueued: " + message.content);
	}

	/**
	 * Sends a stop signal to all dispatcher threads.
	 */
	public void stop() {
		logger.log(Level.INFO, "Stopping message dispatcher.");

		for (int i = 0; i < workers.length; ++i)
			workers[i].stop();
	}

}
