package thread;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import p2p.Constants;


/**
 * A thread that can be started and stopped, or checked if currently running.
 *
 * @author Simeon Andreev
 *
 */
public abstract class Manager implements Runnable {

	/** The logger for this class. */
	protected final Logger logger = Logger.getLogger(Constants.managerlogger);
	/** The thread executing the run method of a deriving class. */
	protected final Thread thread;
	/** Atomic boolean, telling the manager whether it should exit its execution loop. */
	protected final AtomicBoolean condition = new AtomicBoolean(false);
	/** Atomic boolean, true iff the manager is running. */
	protected final AtomicBoolean running = new AtomicBoolean(false);


	/**
	 * Constructor method, to be used only by deriving classes.
	 */
	protected Manager() {
		thread = new Thread(this);
		logger.log(Level.INFO, "Manager object created.");
	}


	/**
	 * Start the manager, executing the run method on the thread.
	 */
	public void start() {
		condition.set(true);
		logger.log(Level.INFO, "Starting waiter thread.");
		// Execute the run method of the deriving class.
		thread.start();
		logger.log(Level.INFO, "Waiter thread started.");
	}

	/**
	 * Stop the manager, waiting for a clean exit.
	 */
	public abstract void stop();

	/**
	 * Returns whether the manager is running or not.
	 *
	 * @return true if the manager is running, otherwise false.
	 */
	public boolean running() {
		final boolean running = this.running.get();
		logger.log(Level.INFO, "Checking if manager thread is running: " + (running ? "yes" : "no"));
		return running;
	}

}
