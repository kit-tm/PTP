package edu.kit.tm.ptp.thread;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A thread that can be started and stopped, or checked if currently running.
 *
 * @author Simeon Andreev
 *
 */
public abstract class Suspendable implements Runnable {

  /** The logger for this class. */
  protected final Logger logger = Logger.getLogger(Suspendable.class.getName());
  /** The thread executing the run method of a deriving class. */
  protected Thread thread;
  /** Atomic boolean, telling the manager whether it should exit its execution loop. */
  protected final AtomicBoolean condition = new AtomicBoolean(false);
  /** Atomic boolean, true iff the manager is running. */
  protected final AtomicBoolean running = new AtomicBoolean(false);


  /**
   * Constructor method, to be used only by deriving classes.
   */
  protected Suspendable() {
    thread = new Thread(this);
    logger.log(Level.INFO, "Suspendable object created.");
  }


  /**
   * Start the suspendable, executing the run method on the thread.
   */
  public void start() throws IOException {
    condition.set(true);
    logger.log(Level.INFO, "Starting suspendable thread.");
    // Execute the run method of the deriving class.
    thread = new Thread(this);
    thread.start();
    logger.log(Level.INFO, "Suspendable thread started.");
  }

  /**
   * Stop the manager, waiting for a clean exit.
   */
  public abstract void stop();

  /**
   * Returns whether the suspendable is running or not.
   *
   * @return true if the suspendable is running, otherwise false.
   */
  public boolean running() {
    final boolean running = this.running.get();
    logger.log(Level.INFO,
        "Checking if suspendable thread is running: " + (running ? "yes" : "no"));
    return running;
  }

}
