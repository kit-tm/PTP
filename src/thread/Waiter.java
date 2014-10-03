package thread;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import p2p.Constants;
import p2p.Listener;


/**
 * A waiter attending to a server socket or a socket connection, executing blocking calls in a separate thread.
 *
 * @author Simeon Andreev
 *
 */
public abstract class Waiter implements Runnable {

	/** The logger for this class. */
	protected final Logger logger = Logger.getLogger(Constants.waiterlogger);
	/** The listener that should be notified of received messages on one of the open sockets. */
	protected final Listener listener;
	/** The thread executing the run method of a deriving class, executing blocking calls. */
	protected final Thread thread;


	/**
	 * Constructor method, to be used only by deriving classes.
	 *
	 * @param listener
	 */
	protected Waiter(Listener listener) {
		this.listener = listener;
		thread = new Thread(this);
		logger.log(Level.INFO, "Waiter object created.");
	}


	/**
	 * Start the waiter, executing the run method on the thread.
	 */
	public void start() {
		logger.log(Level.INFO, "Starting waiter thread.");
		// Execute the run method of the deriving class.
		thread.start();
		logger.log(Level.INFO, "Waiter thread started.");
	}

	/**
	 * Stop the waiter, waiting for a clean exit.
	 *
	 * @throws IOException Thrown if a problem is encountered while closing a socket.
	 */
	public abstract void stop() throws IOException;

}
