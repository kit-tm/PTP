package edu.kit.tm.ptp.raw.receive;

import edu.kit.tm.ptp.ReceiveListener;
import edu.kit.tm.ptp.raw.ConnectionListener;
import edu.kit.tm.ptp.raw.thread.Suspendable;
import edu.kit.tm.ptp.raw.thread.ThreadPool;
import edu.kit.tm.ptp.utility.Constants;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A receiver of API messages. Accepts socket connections with a server socket and uses a thread
 * pool to wait on messages.
 *
 * @author Simeon Andreev
 *
 */
public class MessageReceiver extends Suspendable {

  /** The logger for this class. */
  private final Logger logger = Logger.getLogger(Constants.receiverlogger);
  /** The server socket attended by this waiter. */
  private final ServerSocket server;
  /** The thread pool workers to use. */
  private final ReceiveThread[] workers;
  /** The thread pool to use when receiving from multiple origins. */
  private final ThreadPool<Origin, ReceiveThread> threadPool;


  /**
   * Constructor method.
   *
   * @param listener The listener to notify on newly opened connections.
   * @param port The port number on which to start the server socket.
   * @param threads The number of threads the message receiver may use.
   * @param pollInterval The poll interval (in milliseconds) at which worker threads check the open
   *        sockets for incoming data.
   * @throws IOException Propagates any IOException thrown during the server socket creation.
   */
  public MessageReceiver(ConnectionListener listener, int port, int threads, int pollInterval)
      throws IOException {
    // Create the server socket.
    server = new ServerSocket(port);
    // Create the thread pool.
    workers = new ReceiveThread[threads];
    for (int i = 0; i < threads; ++i) {
      workers[i] = new ReceiveThread(listener, pollInterval);
    }
    threadPool = new ThreadPool<Origin, ReceiveThread>(workers);

    logger.log(Level.INFO, "Message receiver object created with threads number: " + threads);
  }


  @Override
  public void run() {
    running.set(true);
    while (condition.get()) {
      try {
        // Wait on a connection.
        Socket socket = server.accept();
        // Add the socket to the thread pool.
        addConnection(Constants.niaorigin, socket);
        logger.log(Level.INFO, "Message receiver accepted connection.");
      } catch (IOException e) {
        // Stopping the message receiver causes an IOException here, otherwise something went wrong.
        if (condition.get()) {
          logger.log(Level.WARNING,
              "Message receiver caught an IOException while listening for connections: "
                  + e.getMessage());
        }
      }
    }
    running.set(false);
  }

  @Override
  public void stop() {
    logger.log(Level.INFO, "Stopping message dispatcher.");
    if (!running.get()) {
      return;
    }
    condition.set(false);
    threadPool.stop();

    // Close the server socket to wake this thread.
    try {
      server.close();
    } catch (IOException e) {
      // Server socket is already closed. Do nothing.
    }
  }


  /**
   * Adds a socket connection to the thread pool, assigning a thread to poll the connection for
   * incoming data.
   *
   * @param address The Tor hidden service identifier of the connection.
   * @param socket The socket connection to poll for incoming data.
   */
  public synchronized void addConnection(String address, Socket socket) {
    logger.log(Level.INFO, "Adding new connection.");

    // Add the connection handling to the thread.
    ReceiveThread worker = threadPool.getWorker();
    // Enqueue the socket
    worker.enqueue(new Origin(address, socket));
    logger.log(Level.INFO, "Message receiver accepted connection.");
  }

  /**
   * Sets the listener to notify on received messages.
   *
   * @param listener The listener to notify on received messages.
   */
  public void setListener(ReceiveListener listener) {
    for (int i = 0; i < workers.length; ++i) {
      workers[i].setListener(listener);
    }
  }

  /**
   * Returns the port number on which the server socket is open.
   *
   * @return The port number on which the server socket is open.
   */
  public int getPort() {
    return server.getLocalPort();
  }

}
