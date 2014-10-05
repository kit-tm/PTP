package thread;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;

import p2p.Constants;


/**
 * A manager for the personal Tor process of the API.
 *
 * @author Simeon Andreev
 *
 */
public class TorManager extends Manager {

	/** The parameters for the Tor execution command. */
	private final String[] parameters = { Constants.torfile, Constants.torrcoption + " " + Constants.torrcfile };
	/** The managed Tor process. */
	private Process process = null;
	/** The thread reading the output of the Tor process. */
	private Thread output = null;

	/**
	 * Constructor method.
	 */
	public TorManager() {
		super();
		logger.log(Level.INFO, "TorManager object created.");
	}


	/**
	 * @see Runnable
	 */
	@Override
	public void run() {
		logger.log(Level.INFO, "Tor manager thread started.");
		running.set(true);

		try {
			logger.log(Level.INFO, "Executing Tor.");
			logger.log(Level.INFO, "Command: " + parameters[0] + " " + parameters[1]);
			process = Runtime.getRuntime().exec(parameters);
			output = new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						logger.log(Level.INFO, "Output reading thread started.");
						logger.log(Level.INFO, "Fetching the process stream.");
						final BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));

						logger.log(Level.INFO, "Output thread entering reading loop.");
						while (condition.get()) {
							logger.log(Level.INFO, "Output thread waiting on Tor output.");
							String line = input.readLine();
							logger.log(Level.INFO, "Output thread read Tor output line: " + line);
							// If we read null we are done with the process output.
							if (line == null) break;
						}

						logger.log(Level.INFO, "Output thread closing output stream.");
						input.close();
						logger.log(Level.INFO, "Output thread closed output stream.");
					} catch (IOException e) {
						logger.log(Level.WARNING, "Output thread caught an IOException: " + e.getMessage());
					}
				}
			});

			thread.start();
			int result = 0;

			logger.log(Level.INFO, "Tor manager thread entering waiting loop.");
			while (condition.get()) {
				try {
					logger.log(Level.INFO, "Tor manager thread waiting on Tor process to finish.");
					result = process.waitFor();
					logger.log(Level.INFO, "Tor manager thread waiting on output thread to finish.");
					thread.join();
				} catch (InterruptedException e) {
					logger.log(Level.INFO, "Tor manager thread was interrupted during wait.");
				}
			}

			if (result != 0)
				logger.log(Level.WARNING, "Starting Tor process failed with status: " + result);
		} catch (IOException e) {
			logger.log(Level.WARNING, "Tor manager thread caught an IOException when starting Tor: " + e.getMessage());
		}

		running.set(false);
		logger.log(Level.INFO, "Tor manager thread exiting.");
	}

	/**
	 * @see Manager
	 */
	@Override
	public void stop() {
		logger.log(Level.INFO, "Stopping Tor manager thread.");
		condition.set(false);
		logger.log(Level.INFO, "Destroying Tor process.");
		process.destroy();
		logger.log(Level.INFO, "Interrupting Tor manager thread.");
		thread.interrupt();
		logger.log(Level.INFO, "Interrupting output thread.");
		output.interrupt();
		logger.log(Level.INFO, "Stopped Tor manager thread.");
	}

}
