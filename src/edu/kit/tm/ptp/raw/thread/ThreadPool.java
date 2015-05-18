package edu.kit.tm.ptp.raw.thread;


/**
 * A thread pool with limited number of workers. Offers access to the worker with least current estimated load.
 *
 * @author Simeon Andreev
 *
 */
public class ThreadPool<Item, PoolWorker extends Worker<Item>> {

	/** The workers in the thread pool which will handle the message queues. */
	private final PoolWorker workers[];


	/**
	 * Constructor method. Fills the thread pool with already initialized workers.
	 *
	 * @param workers The workers of this thread pool.
	 */
	public ThreadPool(PoolWorker workers[]) {
		this.workers = workers;
	}


	/**
	 * Returns the worker with least current estimated load.
	 * Performs a linear scan to select that worker.
	 *
	 * @return The worker with least current estimated load.
	 */
	public PoolWorker getWorker() {
		long minimumLoad = Long.MAX_VALUE;
		int index = 0;

		// Find a free worker and assign the new queue.
		for (int i = 0; i < workers.length; ++i) {
			if (!workers[i].running()) {
				index = i;
				break;
			}

			final long load = workers[i].load();

			// If no free worker is available, assign the new message to the worker with the least load.
			if (load < minimumLoad) {
				minimumLoad = load;
				index = i;
			}
		}

		return workers[index];
	}


	/**
	 * Stops the workers of this thread pool.
	 */
	public void stop() {
		for (int i = 0; i < workers.length; ++i)
			workers[i].stop();
	}

}
