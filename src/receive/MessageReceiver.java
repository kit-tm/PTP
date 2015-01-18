package receive;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import callback.ReceiveListener;
import callback.ReceiveListenerAdapter;
import thread.Suspendable;
import thread.ThreadPool;
import utility.Constants;


/**
 * A receiver of API messages. Accepts socket connections and uses a thread pool to wait on messages.
 *
 * @author Simeon Andreev
 *
 */
public class MessageReceiver extends Suspendable {

	/** The logger for this class. */
	private final Logger logger = Logger.getLogger(Constants.receiverlogger);
	/** The server socket attended by this waiter. */
	private final ServerSocket server;
	/** The listener to notify on received messages. */
	private ReceiveListener listener = new ReceiveListenerAdapter();
	/** The thread pool to use when receiving from multiple origins. */
	private final ThreadPool<Element, ReceiveThread> threadPool;


	/**
	 * Constructor method.
	 *
	 * @param port The port number on which to start the server socket.
	 * @param threads The number of threads the message receiver may use.
	 * @throws IOException Propagates any IOException thrown during the server socket creation.
	 */
	public MessageReceiver(int port, int threads) throws IOException {
		// Create the server socket.
		server = new ServerSocket(port);
		// Create the thread pool.
		ReceiveThread workers[] = new ReceiveThread[threads];
		for (int i = 0; i < threads; ++i) workers[i] = new ReceiveThread();
		threadPool = new ThreadPool<Element, ReceiveThread>(workers);

		logger.log(Level.INFO, "Message receiver object created with threads number: " + threads);
	}


	/**
	 * @see Suspendable
	 */
	@Override
	public void run() {
		while (condition.get()) {
			try {
				// Wait on a connection.
				Socket socket = server.accept();
				// Add the connection handling to the thread.
				ReceiveThread worker = threadPool.getWorker();
				worker.enqueue(item);
			} catch (IOException e) {
				// Stopping the message receiver causes an IOException here, otherwise something went wrong.
				if (condition.get())
					logger.log(Level.WARNING, "Message receiver caught an IOException while listening for connections: " + e.getMessage());
			}
		}
		running.set(false);
	}

	/**
	 * @see Suspendable
	 */
	@Override
	public void stop() {
		logger.log(Level.INFO, "Stopping message dispatcher.");
		if (!running.get()) return;
		condition.set(false);

		// Close the server socket to wake this thread.
		try {
			server.close();
		} catch (IOException e) {
			// Server socket is already closed. Do nothing.
		}
	}




}
