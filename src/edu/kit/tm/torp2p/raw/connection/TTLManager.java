package edu.kit.tm.torp2p.raw.connection;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.kit.tm.torp2p.Identifier;
import edu.kit.tm.torp2p.raw.ExpireListener;
import edu.kit.tm.torp2p.raw.thread.Suspendable;
import edu.kit.tm.torp2p.utility.Constants;


/**
 * A manager for the open connections. Will notify a listener of connections with TTL below zero.
 *
 * @author Simeon Andreev
 *
 */
public class TTLManager extends Suspendable {


	/**
	 * Convenience wrapper class for the integer timeout.
	 *
	 * @author Simeon Andreev
	 *
	 */
	private static class Timeout {

		/** The TTL left of a socket. */
		public int timer = 0;

	}


	/** The logger for this class. */
	private final Logger logger = Logger.getLogger(Constants.managerlogger);
	/** The client whos connections should be automatically closed. */
	private final ExpireListener listener;
	/** The mapping from identifiers to TTL. */
	private final HashMap<Identifier, Timeout> map = new HashMap<Identifier, Timeout>();
	/** The interval in milliseconds at which the socket TTLs are updated. */
	private final int step;


	/**
	 * Constructor method.
	 *
	 * @param listener The listener that should be notified of expired connection TTLs.
	 * @param step The interval in milliseconds at which the socket TTLs are updated.
	 */
	public TTLManager(ExpireListener listener, int step) {
		this.listener = listener;
		this.step = step;
		logger.log(Level.INFO, "TTLManager object created.");
	}


	/**
	 * @see Runnable
	 */
	@Override
	public void run() {
		logger.log(Level.INFO, "TTLManager entering execution loop.");
		running.set(true);
		while (condition.get()) {
			try {
				// Update the socket TTLs.
				substract();
				// Sleep until the next update.
				Thread.sleep(step);
			} catch (IOException e) {
				logger.log(Level.WARNING, "Received IOException while closing a socket: " + e.getMessage());
			} catch (InterruptedException e) {
				logger.log(Level.INFO, "TTL manager was interrupted while sleeping: " + e.getMessage());
			}
		}
		running.set(false);
		logger.log(Level.INFO, "TTLManager exiting execution loop.");
	}

	/**
	 * @see Suspendable
	 */
	@Override
	public void stop() {
		logger.log(Level.INFO, "Stopping TTLManager.");
		condition.set(false);
		clear();

		while (running.get()) {
			try {
				logger.log(Level.INFO, "Waiting on the manager thread.");
				thread.join();
				logger.log(Level.INFO, "Manager thread finished.");
			} catch (InterruptedException e) {
				logger.log(Level.INFO, "TTL manager was interrupted while waiting for the manager thread: " + e.getMessage());
			}
		}

		logger.log(Level.INFO, "Stopped TTLManager.");
	}

	/**
	 * Adds the given identifier to the connections map.
	 *
	 * @param identifier The identifier that should be added.
	 */
	public synchronized void put(Identifier identifier) {
		logger.log(Level.INFO, "Adding identifier to map: " + identifier.toString());
		map.put(identifier, new Timeout());
	}

	/**
	 * Removes the given identifier from the connections map.
	 *
	 * @param identifier The identifier that should be removed.
	 */
	public synchronized void remove(Identifier identifier) {
		logger.log(Level.INFO, "Removing identifier from map: " + identifier);
		map.remove(identifier);
	}

	/**
	 * Sets the TTL of the socket of a hidden service identifier.
	 *
	 * @param identifier The identifier of the socket.
	 * @param timer The TTL in milliseconds.
	 */
	public synchronized void set(Identifier identifier, int timer) {
		logger.log(Level.INFO, "Setting timeout (" + timer + "ms) for identifier: " + identifier);
		if (!map.containsKey(identifier)) return;

		map.get(identifier).timer = timer;
		logger.log(Level.INFO, "Timeout set.");
	}


	/**
	 * Substracts the amount of milliseconds from the TTLs of open sockets.
	 *
	 * @throws IOException Propagates any IOException the API received while disconnecting a hidden service identifier.
	 */
	private synchronized void substract() throws IOException {
		LinkedList<Identifier> closed = new LinkedList<Identifier>();

		// Iterate over the identifiers and substract the step from the socket TTLs.
		for (Entry<Identifier, Timeout> entry : map.entrySet()) {
			entry.getValue().timer -= step;
			// Check if the TTL expired after the substraction.
			if (entry.getValue().timer >= 0) continue;

			logger.log(Level.INFO, "TTL expired for identifier: " + entry.getKey());
			// Disconnect the socket.
			listener.expired(entry.getKey());
			// Add the identifier to be removed from the map.
			closed.add(entry.getKey());
		}

		for (Identifier identifier : closed)
			map.remove(identifier);
	}

	/**
	 * Clears the connections map.
	 */
	private synchronized void clear() {
		map.clear();
	}

}
