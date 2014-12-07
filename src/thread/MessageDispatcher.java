package thread;

import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import p2p.Message;
import p2p.SendListener;


/**
 * A dispatcher for API messages. Keeps a queue of unsent messages and attempts to re-send them at a given interval.
 * Sleeps while no messages are present in the queue.
 *
 * @author Simeon Andreev
 *
 */
public class MessageDispatcher extends Suspendable {


	/**
	 * A listener that will be notified of a message sending attempt.
	 *
	 * @author Simeon Andreev
	 *
	 */
	public static interface Listener {

		/**
		 * Notifies this listener that a message sending should be attempted.
		 *
		 * @param message The message to send.
		 * @param lisener The listener to be notified of sending events.
		 * @param timeout The timeout for the sending.
		 * @param elapsed The amount of time the message has waited so far.
		 * @return false if a further attempt to send the message should be done.
		 */
		public boolean dispatch(Message message, SendListener lisener, long timeout, long elapsed);

	}



	/**
	 * A wrapper class for queue elements.
	 *
	 * @author Simeon Andreev
	 */
	private static class QueueElement {

		/** The message stored in the queue element. */
		public final Message message;
		/** The timeout for the sending. */
		public final long timeout;
		/** The listener to notify of sending events. */
		public final SendListener listener;
		/** Waiting time of the message. */
		public long elapsed = 0;


		/**
		 * Constructor method.
		 *
		 * @param message The message to send with the queue element.
		 * @param timeout The timeout for the sending.
		 * @param listener The listener to notify of sending events.
		 */
		public QueueElement(Message message, long timeout, SendListener listener) {
			this.message = message;
			this.timeout = timeout;
			this.listener = listener;
		}

	}


	/** The message queue. */
	private final LinkedList<QueueElement> queue = new LinkedList<QueueElement>();
	/** The listener that should be notified when a message is to be sent. */
	private final Listener listener;
	/** A lock on which to wait while no messages are present in the queue. */
	private final Lock lock = new ReentrantLock();
	/** The condition on which to wait while no messages are present in the queue. */
	final Condition empty = lock.newCondition();
	/** The interval (in milliseconds) at which the dispatcher should attempt to send the queue messages. */
	private int poll;


	/**
	 * Constructor method.
	 *
	 * @param listener The parameters of this dispatcher.
	 * @param client The raw API client to use when sending messages.
	 * @param manager The TTL manager to notify of established connections.
	 */
	public MessageDispatcher(Listener listener, int poll) {
		this.listener = listener;
		this.poll = poll;
		logger.log(Level.INFO, "Message dispatcher object created.");
	}


	/**
	 * Adds a message to the front of the queue and wakes the dispatcher, so that the message can be sent.
	 *
	 * @param message The message to add to the queue for sending.
	 * @param timeout The timeout for the sending.
	 * @param listener The listener to notify of sending events.
	 */
	public synchronized void enqueueMessage(Message message, long timeout, SendListener listener) {
		// Add the message to the front of the queue.
		queue.addFirst(new QueueElement(message, timeout, listener));
		// Wake the dispatching thread.
		lock.lock();
		empty.signal();
		lock.unlock();
		logger.log(Level.INFO, "Message enqueued: " + message.content);
	}


	/**
	 * @see Runnable
	 */
	@Override
	public void run() {
		running.set(true);
		logger.log(Level.INFO, "Dispatcher thread entering its execution loop.");
		while (condition.get()) {
			while (queue.isEmpty() && condition.get()) {
				try {
					logger.log(Level.INFO, "Dispatcher thread waiting on messages.");
					lock.lock();
					empty.await();
					lock.unlock();
				} catch (InterruptedException e) {
					// Sleeping was interrupted. Do nothing.
				}
			}

			// While the queue is not empty, attempt to send all messages in it.
			long sleepDuration = 0;
			while (!queue.isEmpty() && condition.get()) {
				dispatchMessages(sleepDuration);

				// Sleep until the next attempts.
				final long start = System.currentTimeMillis();
				try {
					Thread.sleep(poll);
				} catch (InterruptedException e) {

				}
				sleepDuration = System.currentTimeMillis() - start;
			}
		}
		running.set(false);
		logger.log(Level.INFO, "Dispatcher thread exiting its execution loop.");
	}

	/**
	 * @see Suspendable
	 */
	@Override
	public void stop() {
		logger.log(Level.INFO, "Stopping message dispatcher.");
		condition.set(false);
		thread.interrupt();

		// Wait for the dispatching thread to exit.
		while (running.get()) {
			try {
				Thread.sleep(1 * 1000);
			} catch (InterruptedException e) {
				// Sleeping was interrupted. Do nothing.
			}
		}
	}


	/**
	 * Attempts to send all messages in the queue.
	 *
	 * @param sleepDuration
	 */
	private synchronized void dispatchMessages(long sleepDuration) {
		logger.log(Level.INFO, "Message dispatcher attempting to send messages.");

		LinkedList<QueueElement> remaining = new LinkedList<QueueElement>();

		for (QueueElement element : queue) {
			logger.log(Level.INFO, "Message dispatcher attempting to send message: " + element.message.content + " to " + element.message.destination.getTorAddress());
			element.elapsed += sleepDuration;
			final boolean done = listener.dispatch(element.message, element.listener, element.timeout, element.elapsed);
			if (!done) remaining.push(element);
		}

		queue.clear();
		queue.addAll(remaining);
		logger.log(Level.INFO, "Message dispatcher has " + queue.size() + " message(s) left.");
	}

}
