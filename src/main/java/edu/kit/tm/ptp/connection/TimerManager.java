package edu.kit.tm.ptp.connection;

import edu.kit.tm.ptp.Identifier;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A class which allows to set/remove timers and be informed when they expire.
 *
 * @author Timon Hackenjos
 * @author Simeon Andreev
 *
 */
public class TimerManager implements Runnable  {

  /** The logger for this class. */
  private final Logger logger = Logger.getLogger(TimerManager.class.getName());
  private final ExpireListener listener;
  private final HashMap<TimerKey, Integer> map = new HashMap<TimerKey, Integer>();
  /** The interval in milliseconds at which the values are updated. */
  private final int step;
  private final Thread thread;
  
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
   * @param listener The listener that should be notified of expired connection timers.
   * @param step The interval in milliseconds at which the timer values are updated.
   */
  public TimerManager(ExpireListener listener, int step) {
    this(listener, step, null);
  }


  /**
   * Constructor method.
   *
   * @param listener The listener that should be notified of expired connection timers.
   * @param step The interval in milliseconds at which the timer values are updated.
   */
  public TimerManager(ExpireListener listener, int step, ThreadGroup group) {
    this.listener = listener;
    this.step = step;
    this.thread = new Thread(group, this);
    logger.log(Level.INFO, "TimerManager object created.");
  }

  @Override
  public void run() {
    logger.log(Level.INFO, "TimerManager entering execution loop.");

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
        // Update the timer values
        substract();
      } catch (IOException e) {
        logger.log(Level.WARNING, "Received IOException while closing a socket: " + e.getMessage());
      }
    }
    logger.log(Level.INFO, "TimerManager exiting execution loop.");
  }
  
  /**
   * Start the TimerManager.
   */
  public void start() {
    logger.log(Level.INFO, "Starting TimerManager");
    thread.start();
    logger.log(Level.INFO, "TimerManager started");
  }

  /**
   * Stops the TimerManager and clears timeouts.
   * Does nothing if the manager has been stopped before.
   */
  public void stop() {
    logger.log(Level.INFO, "Stopping TimerManager.");
    thread.interrupt();

    try {
      thread.join();
    } catch (InterruptedException e) {
      logger.log(Level.INFO, "TimerManager was interrupted while waiting for the thread");
    }
    
    clear();

    logger.log(Level.INFO, "Stopped TimerManager.");
  }

  /**
   * Removes the timeout with the specified class and identifier.
   *
   * @param identifier The identifier that should be removed.
   */
  public synchronized void remove(Identifier identifier, int timerClass) {
    logger.log(Level.INFO, "Removing identifier from map: " + identifier);
    map.remove(new TimerKey(identifier, timerClass));
  }

  /**
   * Schedules a new timer if none is set already.
   *
   * @param identifier The identifier for the timeout.
   * @param timer The delay in milliseconds.
   * @param timerClass Identifies different timers of the same identifier.
   */
  public synchronized void setTimerIfNoneExists(Identifier identifier, int timer, int timerClass) {
    TimerKey key = new TimerKey(identifier, timerClass);
    if (!map.containsKey(key)) {
      logger.log(Level.INFO, "Setting timeout (" + timer + "ms) for identifier: " + identifier
          + " class: " + timerClass);
      map.put(key, timer);
    }
  }
  
  /**
   * Schedules a new timer. Overwrites existing ones with the same timerClass and identifier.
   *
   * @param identifier The identifier for the timeout.
   * @param timer The delay in milliseconds.
   * @param timerClass Identifies different timers of the same identifier.
   */
  public synchronized void setTimer(Identifier identifier, int timer, int timerClass) {
    TimerKey key = new TimerKey(identifier, timerClass);
    logger.log(Level.INFO, "Setting timeout (" + timer + "ms) for identifier: " + identifier
          + " class: " + timerClass);
    map.put(key, timer);
  }


  /**
   * Substracts the amount of milliseconds from the timers.
   *
   */
  private synchronized void substract() throws IOException {
    LinkedList<TimerKey> closed = new LinkedList<TimerKey>();
    int timer;

    // Iterate over the entries and substract the step
    for (Entry<TimerKey, Integer> entry : map.entrySet()) {      
      timer = entry.getValue() - step;
      entry.setValue(timer);
      
      // Check if the timer expired after the substraction.
      if (timer >= 0) {
        continue;
      }

      listener.expired(entry.getKey().identifier, entry.getKey().timeoutClass);
      closed.add(entry.getKey());
    }

    for (TimerKey key : closed) {
      map.remove(key);
    }
  }

  /**
   * Clears all timeouts.
   */
  private synchronized void clear() {
    map.clear();
  }
  
  public boolean isRunning() {
    return thread.isAlive();
  }

}
