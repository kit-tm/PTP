package thread;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.logging.Level;

import p2p.Listener;


/**
 * A waiter attending a server socket. Will wait for new connections and maintain a socket waiter for each socket.
 *
 * @author Simeon Andreev
 *
 * @see Waiter
 */
public class ServerWaiter extends Waiter {

	/** The server socket attended by this waiter. */
	private final ServerSocket server;
	/** The list of socket threads attending the open sockets. */
	private final LinkedList<SocketWaiter> waiters = new LinkedList<SocketWaiter>();


	/**
	 * Constructor method.
	 *
	 * @param port The port number on which the server socket should be opened.
	 * @throws IOException Thrown if unable to create a server socket on the specified port.
	 */
	public ServerWaiter(int port) throws IOException {
		super();
		logger.log(Level.INFO, "Serveer starting on: " + port);
		this.server = new ServerSocket(port);
		logger.log(Level.INFO, "ServerWaiter object created.");
	}


	/**
	 * @see Runnable
	 */
	@Override
	public void run() {
		logger.log(Level.INFO, "Server thread started.");
		try {
			logger.log(Level.INFO, "Server thread entering its execution loop.");
			while (true) {
				logger.log(Level.INFO, "Server thread waiting on a socket connection.");
				Socket client = server.accept();
				logger.log(Level.INFO, "Server thread accepted socket connection: " + client.getInetAddress().toString());
				SocketWaiter waiter = new SocketWaiter(client);
				waiter.set(listener);
				waiter.start();
				logger.log(Level.INFO, "Server thread adding a waiter for the new socket.");
				waiters.add(waiter);
			}
		} catch (SocketException e) {
			logger.log(Level.WARNING, "Server thread received a SocketException during a server socket operation: " + e.getMessage());
		} catch (IOException e) {
			logger.log(Level.WARNING, "Server thread received an IOException during a server socket operation: " + e.getMessage());
		}
		logger.log(Level.INFO, "Server thread exiting run method.");
	}

	/**
	 * @see Waiter
	 */
	@Override
	public void set(Listener listener) {
		this.listener = listener;
		for (Waiter waiter : waiters) waiter.set(listener);
	}

	/**
	 * @see Waiter
	 */
	@Override
	public void stop() throws IOException {
		if (server.isClosed()) return;

		logger.log(Level.INFO, "Server thread stopping the socket threads.");
		for (SocketWaiter waiter : waiters) waiter.stop();
		logger.log(Level.INFO, "Clearing socket threads list.");
		waiters.clear();

		logger.log(Level.INFO, "Closing server socket.");
		server.close();
		logger.log(Level.INFO, "Server thread stopped.");
	}

}
