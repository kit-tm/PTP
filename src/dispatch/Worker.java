package dispatch;

import java.util.concurrent.ConcurrentLinkedQueue;

import callback.DispatchListener;
import thread.Suspendable;


/**
 * TODO: write
 *
 * @author Simeon Andreev
 *
 * @see Suspendable
 *
 */
public class Worker extends Suspendable {

	/** The listener that should be notified when a message is to be sent. */
	private final DispatchListener listener;


	/**
	 * Constructor method.
	 *
	 * @param listener The listener that should be notified when a message is to be sent.
	 */
	public Worker(DispatchListener listener) {
		this.listener = listener;
	}


	/**
	 * TODO: write
	 *
	 * @param queue
	 */
	public void add(ConcurrentLinkedQueue<Element> queue) {
		// TODO: implement
	}

	/**
	 * TODO: write
	 *
	 * @return
	 */
	public long load() {
		return 0;
	}

	/**
	 * @see Suspendable
	 */
	@Override
	public void run() {
		// TODO: implement
	}

	/**
	 * @see Suspendable
	 */
	@Override
	public void stop() {
		// TODO: implement
	}

}
