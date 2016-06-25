package edu.kit.tm.ptp.connection;

import edu.kit.tm.ptp.Identifier;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A manager for the open connections. Will notify a listener of connections with TTL below zero.
 *
 * @author Timon Hackenjos
 * @author Simeon Andreev
 *
 */
public class TimerManager implements Runnable  {

  /** The logger for this class. */
  private final Logger logger = Logger.getLogger(TimerManager.class.getName());
  /** The client whos connections should be automatically closed. */
  private final ExpireListener listener;
  /** The mapping from identifiers to TTL. */
  private final HashMap<TimerKey, Integer> map = new HashMap<TimerKey, Integer>();
  /** The interval in milliseconds at which the socket TTLs are updated. */
  private final int step;
  private Thread thread = new Thread(this);
  
  private static final class TimerKey {
    public Identifier identifier;
    public int timeoutClass;
    
    public TimerKey(Identifier identifier, int timeoutClass) {
      this.identifier = identifier;
      this.timeoutClass = timeoutClass;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((identifier == null) ? 0 : identifier.hashCode());
      result = prime * result + timeoutClass;
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      
      TimerKey other = (TimerKey) obj;
      if (identifier == null) {
        if (other.identifier != null) {
          return false;
        }
      } else if (!identifier.equals(other.identifier)) {
        return false;
      }
      
      if (timeoutClass != other.timeoutClass) {
        return false;
      }
      
      return true;
    }
  }


  /**
   * Constructor method.
   *
   * @param listener The listener that should be notified of expired connection TTLs.
   * @param step The interval in milliseconds at which the socket TTLs are updated.
   */
  public TimerManager(ExpireListener listener, int step) {
    this.listener = listener;
    this.step = step;
    logger.log(Level.INFO, "TTLManager object created.");
  }

  @Override
  public void run() {
    logger.log(Level.INFO, "TTLManager entering execution loop.");

    while (!thread.isInterrupted()) {
      long start = System.currentTimeMillis();
      long elapsed = 0;

      // Sleep until the next update.
      do {
        try {
          Thread.sleep(step - elapsed);
        } catch (InterruptedException e) {
          // Thread should stop
          return;
        }
        elapsed = System.currentTimeMillis() - start;
      } while (elapsed < step);

      try {
        // Update the socket TTLs.
        substract();
      } catch (IOException e) {
        logger.log(Level.WARNING, "Received IOException while closing a socket: " + e.getMessage());
      }
    }
    logger.log(Level.INFO, "TTLManager exiting execution loop.");
  }
  
  /**
   * Start the TTLManager.
   */
  public void start() {
    logger.log(Level.INFO, "Starting TTLManager");
    thread.start();
    logger.log(Level.INFO, "TTLManager started");
  }

  /**
   * Stops the TTLManager and clears timeouts.
   * Does nothing if the manager has been stopped before.
   */
  public void stop() {
    logger.log(Level.INFO, "Stopping TTLManager.");
    thread.interrupt();

    try {
      thread.join();
    } catch (InterruptedException e) {
      logger.log(Level.INFO, "TTL manager was interrupted while waiting for the thread");
    }
    
    clear();

    logger.log(Level.INFO, "Stopped TTLManager.");
  }

  /**
   * Removes the given identifier from the connections map.
   *
   * @param identifier The identifier that should be removed.
   */
  public synchronized void remove(Identifier identifier, int timerClass) {
    logger.log(Level.INFO, "Removing identifier from map: " + identifier);
    map.remove(new TimerKey(identifier, timerClass));
  }

  /**
   * Sets the TTL of the socket of a hidden service identifier.
   *
   * @param identifier The identifier of the socket.
   * @param timer The TTL in milliseconds.
   */
  public synchronized void set(Identifier identifier, int timer, int timerClass) {
    TimerKey key = new TimerKey(identifier, timerClass);
    if (!map.containsKey(key)) {
      logger.log(Level.INFO, "Setting timeout (" + timer + "ms) for identifier: " + identifier
          + " class: " + timerClass);
      map.put(key, timer);
    }
  }


  /**
   * Substracts the amount of milliseconds from the TTLs of open sockets.
   *
   * @throws IOException Propagates any IOException the API received while disconnecting a hidden
   *         service identifier.
   */
  private synchronized void substract() throws IOException {
    LinkedList<TimerKey> closed = new LinkedList<TimerKey>();
    int timer;

    // Iterate over the identifiers and substract the step from the socket TTLs.
    for (Entry<TimerKey, Integer> entry : map.entrySet()) {      
      timer = entry.getValue() - step;
      entry.setValue(timer);
      
      // Check if the TTL expired after the substraction.
      if (timer >= 0) {
        continue;
      }

      // Disconnect the socket.
      listener.expired(entry.getKey().identifier, entry.getKey().timeoutClass);
      // Add the identifier to be removed from the map.
      closed.add(entry.getKey());
    }

    for (TimerKey key : closed) {
      map.remove(key);
    }
  }

  /**
   * Clears the connections map.
   */
  private synchronized void clear() {
    map.clear();
  }
  
  public boolean isRunning() {
    return thread.isAlive();
  }

}
