package thread;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import p2p.Client;
import p2p.Constants;


/**
 * TODO: write
 *
 * @author Simeon Andreev
 *
 */
public class TTLManager implements Runnable {


	/**
	 * TODO: write
	 *
	 * @author Simeon Andreev
	 *
	 */
	private static class Timeout {

		/** TODO: write */
		public int timer = 0;

	}
	// TODO: code comments and logging


	/** The logger for this class. */
	private final Logger logger = Logger.getLogger(Constants.managerlogger);
	/** TODO: write */
	private final Client client;
	/** TODO: write */
	private final HashMap<String, Timeout> map = new HashMap<String, Timeout>();
	/** TODO: write */
	private final Thread thread;
	/** TODO: write */
	private final AtomicBoolean condition = new AtomicBoolean(false);
	/** TODO: write */
	private final AtomicBoolean running = new AtomicBoolean(false);
	/** TODO: write */
	private final int step;


	/**
	 * TODO: write
	 *
	 * @param client
	 * @param step
	 */
	public TTLManager(Client client, int step) {
		this.client = client;
		this.step = step;
		thread = new Thread(this);
		thread.start();
	}


	/**
	 * @see Runnable
	 */
	@Override
	public void run() {
		running.set(true);
		while (condition.get()) {
			try {
				substract();
				Thread.sleep(2 * 1000 /* TODO: parameter for this */);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		running.set(false);
	}

	/**
	 * TODO: write
	 */
	public void stop() {
		condition.set(false);
		clear();

		while (running.get()) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * TODO: write
	 *
	 * @param identifier
	 */
	public synchronized void put(String identifier) {
		map.put(identifier, new Timeout());
	}

	/**
	 * TODO: write
	 *
	 * @param identifier
	 */
	public synchronized void remove(String identifier) {
		map.remove(identifier);
	}

	/**
	 * TODO: write
	 *
	 * @param identifier
	 * @param timer
	 */
	public synchronized void set(String identifier, int timer) {
		if (!map.containsKey(identifier)) return;

		map.get(identifier).timer = timer;
	}


	/**
	 * TODO: write
	 *
	 * @throws IOException
	 */
	private synchronized void substract() throws IOException {
		LinkedList<String> closed = new LinkedList<String>();

		for (Entry<String, Timeout> entry : map.entrySet()) {
			entry.getValue().timer -= step;
			if (entry.getValue().timer >= 0) continue;

			client.disconnect(entry.getKey());
			closed.add(entry.getKey());
		}

		for (String identifier : closed)
			map.remove(identifier);
	}

	/**
	 * TODO: write
	 */
	private synchronized void clear() {
		map.clear();
	}

}
