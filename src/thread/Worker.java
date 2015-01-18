package thread;


/**
 * A suspendable thread which executes tasks and offers load estimation.
 *
 * @author Simeon Andreev
 *
 */
public abstract class Worker<Item> extends Suspendable {


	/** The current load of this thread. */
	protected long load = 0;


	/**
	 * Adds a task to be executed by this worker.
	 *
	 * @param item The task information holder.
	 */
	public abstract void enqueue(Item item);


	/**
	 * Returns the current load estimate of this worker.
	 *
	 * @return The current load estimate of this worker.
	 */
	public long load() {
		return load;
	}


	/**
	 * @see Suspendable
	 */
	@Override
	public void stop() {
		if (!running.get()) return;
		condition.set(false);
	}

}
