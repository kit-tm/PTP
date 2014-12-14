package dispatch;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

import callback.DispatchListener;
import thread.Suspendable;


/**
 * TODO: write
 *
 * @author Simeon Andreev
 *
 * @see Suspendable
 *
 */
public class Worker extends Suspendable {


	private static class Queue {

		public String destination;
		public ConcurrentLinkedQueue<Element> queue;


		public Queue(String destination, ConcurrentLinkedQueue<Element> queue) {
			this.destination = destination;
			this.queue = queue;
		}

	}


	public static interface Listener {

		public void done(String destination);

	}


	/** The listener that should be notified when a message is to be sent. */
	private final DispatchListener dispatchLisener;
	/** The listener that should be notified when a queue is empty. */
	private final Listener doneListener;
	/** The set of queues that this thread handles. */
	private LinkedList<Queue> queues = new LinkedList<Queue>();
	/** The position of the current queue. */
	private int current = 0;
	/** The current load of this thread. */
	private long load = 0;

	/**
	 * Constructor method.
	 *
	 * @param listener The listener that should be notified when a message is to be sent.
	 */
	public Worker(DispatchListener dispatchLisener, Listener doneListener) {
		this.dispatchLisener = dispatchLisener;
		this.doneListener = doneListener;
	}


	/**
	 * Adds a queue of messages to this worker, to dispatch messages from.
	 *
	 * @param queue The queue of messages.
	 */
	public synchronized void addQueue(String destination, ConcurrentLinkedQueue<Element> queue) {
		queues.add(new Queue(destination, queue));
	}

	/**
	 * Adds a messages to dispatch in the appropriate queue for the destination.
	 *
	 * @param queue The queue of messages.
	 */
	public synchronized void addMessage(String destination, Element element) {
		// Update the threads load.
		load += element.message.content.length();

		// Check if the thread already has a queue for the destination.
		for (Queue current : queues)
			if (current.destination.equals(destination)) {
				current.queue.add(element);
				return;
			}

		// Otherwise, add a new queue.
		ConcurrentLinkedQueue<Element> queue = new ConcurrentLinkedQueue<Element>();
		queue.add(element);
		queues.add(new Queue(destination, queue));
	}

	/**
	 * Returns the current load (total message length to dispatch) of this worker.
	 *
	 * @return The current load of this worker.
	 */
	public synchronized long load() {
		return load;
	}

	/**
	 * @see Suspendable
	 */
	@Override
	public void start() {
		if (running.get()) return;

		// We require running to be true as soon as we leave this method.
		running.set(true);

		condition.set(true);
		logger.log(Level.INFO, "Starting suspendable thread.");
		// We also require re-runnable objects.
		thread = new Thread(this);
		thread.start();
		logger.log(Level.INFO, "Suspendable thread started.");
	}

	/**
	 * @see Suspendable
	 */
	@Override
	public void run() {
		// Loop and dispatch a message from each queue in rounds.
		while (condition.get() && !empty()) {
			Queue current = pick();

			Element element = current.queue.peek();
			// Notify the listener of the message dispatch.
			final boolean sent = dispatchLisener.dispatch(element.message, element.listener, element.timeout, System.currentTimeMillis() - element.timestamp);

			// If the listener does not allow to remove the message from the queue, continue.
			if (!sent) continue;

			load -= element.message.content.length();

			// Otherwise, remove the message from the queue.
			current.queue.remove();
			if (current.queue.isEmpty()) {
				// If the queue is now empty, tell the other listener that the queue is empty.
				doneListener.done(current.destination);
				remove();
			}
		}
		running.set(false);
	}

	/**
	 * @see Suspendable
	 */
	@Override
	public void stop() {
		if (!running.get()) return;
		condition.set(false);
	}


	/**
	 * Checks whether the thread has no queues to process.
	 *
	 * @return true iff the thread has no queues to process.
	 */
	private synchronized boolean empty() {
		return queues.isEmpty();
	}

	/**
	 * Returns the current message queue from which the worker should dispatch a message.
	 *
	 * @return The current message queue to dispatch a message from.
	 */
	private synchronized Queue pick() {
		// Update current index.
		current = (current + 1) % queues.size();
		// Return the next queue from which the worker should dispatch a message.
		return queues.get(current);
	}

	/**
	 * Removes the current message queue from the set of queues.
	 */
	private synchronized void remove() {
		// Return the next queue from which the worker should dispatch a message.
		queues.remove(current);
		// Update current index.
		--current;
	}

}
