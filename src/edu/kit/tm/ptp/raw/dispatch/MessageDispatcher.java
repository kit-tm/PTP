package edu.kit.tm.ptp.raw.dispatch;

import edu.kit.tm.ptp.Message;
import edu.kit.tm.ptp.SendListener;
import edu.kit.tm.ptp.raw.DispatchListener;
import edu.kit.tm.ptp.raw.thread.ThreadPool;
import edu.kit.tm.ptp.utility.Constants;

import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A dispatcher for API messages. Keeps a queue of unsent messages per destination and attempts to
 * (re-)send the messages using a thread pool.
 *
 * @author Simeon Andreev
 *
 */
public class MessageDispatcher {

  /** The logger for this class. */
  private final Logger logger = Logger.getLogger(Constants.dispatcherlogger);
  /** The set of message queues for all destinations. */
  private final HashMap<String, DispatchThread> map = new HashMap<String, DispatchThread>();
  /** The set of destinations with empty message queues. */
  private final ConcurrentLinkedQueue<String> removed = new ConcurrentLinkedQueue<String>();
  /** The thread pool to use when dispatching to multiple destinations. */
  private final ThreadPool<Element, DispatchThread> threadPool;
  /** The listener that will be notified of empty destination queues. */
  private final DispatchThread.Listener doneListener = new DispatchThread.Listener() {

    @Override
    public void done(String destination) {
      remove(destination);
    }

  };


  /**
   * Constructor method.
   *
   * @param dispatchListener The listener to notify on message dispatch.
   * @param threads The number of threads this dispatcher may use.
   * @param dispatchInterval The time to wait (in milliseconds) between message dispatches within a
   *        dispatcher thread.
   */
  public MessageDispatcher(DispatchListener dispatchListener, int threads, long dispatchInterval) {
    // Create the thread pool.
    DispatchThread[] workers = new DispatchThread[threads];
    for (int i = 0; i < threads; ++i) {
      workers[i] = new DispatchThread(dispatchListener, doneListener, dispatchInterval);
    }
    threadPool = new ThreadPool<Element, DispatchThread>(workers);

    logger.log(Level.INFO, "Message dispatcher object created with threads number: " + threads);
  }


  /**
   * Adds a message to a destination queue and sets a thread to handle the message dispatch.
   *
   * @param message The message to add to the queue for sending.
   * @param timeout The timeout for the sending.
   * @param listener The listener to notify of sending events.
   */
  public synchronized void enqueueMessage(Message message, long timeout, SendListener listener) {
    String destination = message.identifier.getTorAddress();
    Element element = new Element(message, timeout, listener);

    // Update the destination set by removing destinations with empty queues.
    while (!removed.isEmpty()) {
      String d = removed.poll();
      map.remove(d);
    }

    // Find a worker for the message.
    if (!map.containsKey(destination)) {
      DispatchThread worker = threadPool.getWorker();
      // Add the message to the chosen worker and map the destination to that worker.
      worker.enqueue(element);
      map.put(destination, worker);
    } else {
      map.get(destination).enqueue(element);
    }

    logger.log(Level.INFO,
        "Message enqueued: " + message.content.substring(0, message.content.length() % 25) + " ("
            + message.identifier + ")");
  }

  /**
   * Sends a stop signal to all dispatcher threads.
   */
  public void stop() {
    logger.log(Level.INFO, "Stopping message dispatcher.");
    threadPool.stop();
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
