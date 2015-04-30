package edu.kit.tm.torp2p.raw.dispatch;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import edu.kit.tm.torp2p.raw.DispatchListener;
import edu.kit.tm.torp2p.raw.thread.Worker;


/**
 * A thread pool worker which dispatches messages to specific destinations.
 * Maintains a single queue per destination and alternates between queues
 * when dispatching to multiple destinations.
 *
 * @author Simeon Andreev
 *
 * @see Worker
 *
 */
public class DispatchThread extends Worker<Element> {


	/**
	 * A listener to notify whenever a destination has no more messages to dispatch.
	 *
	 * @author Simeon Andreev
	 *
	 */
	public static interface Listener {

		/**
		 * Notifies the listener that a destination has no more messages to dispatch.
		 *
		 * @param destination The destinations with no more messages.
		 */
		public void done(String destination);

	}


	/** The listener that should be notified when a message is to be sent. */
	private final DispatchListener dispatchListener;
	/** The listener that should be notified when a queue is empty. */
	private final Listener doneListener;
	/** The set of queues that this thread handles. */
	private HashMap<String, LinkedList<Element>> queues = new HashMap<String, LinkedList<Element>>();
	/** The set of destinations that this thread handles. */
	private LinkedList<String> destinations = new LinkedList<String>();
	/** A concurrent queue with messages that are not yet distributed. */
	private ConcurrentLinkedQueue<Element> undistributed = new ConcurrentLinkedQueue<Element>();
	/** An atomic integer that keeps count of the undistributed messages.  */
	private AtomicInteger counter = new AtomicInteger(0);
	/** The current load of this thread. */
	private long load = 0;
	/** The time to wait (in milliseconds) between message dispatches. */
	private long dispatchInterval;

	/**
	 * Constructor method.
	 *
	 * @param dispatchListener The listener that should be notified when a message is to be sent.
	 * @param doneListener The listener that should be notified when a message queue for a destination goes empty.
	 * @param dispatchInterval The time to wait (in milliseconds) between message dispatches.
	 */
	public DispatchThread(DispatchListener dispatchListener, Listener doneListener, long dispatchInterval) {
		this.dispatchListener = dispatchListener;
		this.doneListener = doneListener;
		this.dispatchInterval = dispatchInterval;
	}


	/**
	 * Adds a messages to dispatch in the appropriate queue for its destination.
	 * Starts the dispatch loop if this thread is not running.
	 *
	 * @param message The message and auxiliary objects.
	 */
	public synchronized void enqueue(Element message) {
		undistributed.add(message);
		counter.incrementAndGet();

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
		}
	}

	/**
	 * Returns the current load (total message length to dispatch) of this worker.
	 *
	 * @return The current load of this worker.
	 */
	public long load() {
		return load;
	}


	/**
	 * @see Worker
	 */
	@Override
	public void run() {
		// Loop and dispatch a message from each queue in rounds.
		while (condition.get()) {
			// Check for new messages.
			distributeMessages();

			// Pick a destination queue to send a message from.
			LinkedList<Element> current = pick();

			// Fetch the first messages of the queue.
			Element element = current.getFirst();
			// Note the start of the dispatch.
			final long start = System.currentTimeMillis();
			// Notify the listener of the message dispatch.
			final boolean sent = dispatchListener.dispatch(element.message, element.listener, element.timeout, System.currentTimeMillis() - element.timestamp);

			// If the listener does not allow to remove the message from the queue, continue.
			if (!sent) {
				// Wait before trying the next dispatch.
				long elapsed = System.currentTimeMillis() - start;
				while (elapsed < dispatchInterval) {
					try {
						Thread.sleep(dispatchInterval - elapsed);
						elapsed = System.currentTimeMillis() - start;
					} catch (InterruptedException e) {
						// Thread sleeping was interrupted. Do nothing.
					}
				}

				continue;
			}

			// Update the thread load.
			load -= element.message.content.length();

			// Otherwise, remove the message from the queue.
			current.removeFirst();
			// Check if we ran out of messages to add.
			if (current.isEmpty()) {
				remove(element.message.identifier.getTorAddress());

				// If there are more destination queues, no need to check for execution stop.
				if (!queues.isEmpty()) continue;
				stopCheck();
			}
		}
	}


	/**
	 * Checks if this thread has no more undisturbed messages, and if so indicates that execution should stop.
	 */
	private synchronized void stopCheck() {
		// Check if we have more messages to send.
		if (!undistributed.isEmpty()) return;

		// Otherwise, exit the execution loop.
		condition.set(false);
		running.set(false);
	}

	/**
	 * Sends all current undistributed messages to their destination queues.
	 */
	private void distributeMessages() {
		int n = counter.get();

		// Remove all n elements from the undistributed message queue.
		while (n > 0) {
			Element element = undistributed.poll();
			String destination = element.message.identifier.getTorAddress();
			// Update the threads load.
			load += element.message.content.length();

			// Check if the thread already has a queue for the destination.
			if (queues.containsKey(destination)) {
				queues.get(destination).addLast(element);
			// Otherwise, add a new queue.
			} else {
				LinkedList<Element> queue = new LinkedList<Element>();
				queue.add(element);
				queues.put(destination, queue);
				destinations.addLast(destination);
			}

			// Move to the next element.

			--n;
			counter.decrementAndGet();
		}
	}

	/**
	 * Returns the current message queue from which the worker should dispatch a message.
	 *
	 * @return The current message queue to dispatch a message from.
	 */
	private LinkedList<Element> pick() {
		// Pick the current front of the queue, and push it at the back of the queue.
		final String next = destinations.removeFirst();
		destinations.addLast(next);

		// Return the next queue from which the worker should dispatch a message.
		return queues.get(next);
	}

	/**
	 * Removes current message queue can be removed.
	 *
	 * @param destination The destination of the current queue.
	 */
	private void remove(String destination) {
		// Remove the current destination for message dispatches.
		destinations.removeLast();
		// Remove the current queue from which the worker dispatched a message.
		queues.remove(destination);

		// If the queue is now empty, tell the other listener that the queue is empty.
		doneListener.done(destination);
	}

}
