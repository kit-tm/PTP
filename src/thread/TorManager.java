package thread;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import p2p.Constants;


/**
 * A manager for the personal Tor process of the API.
 *
 * @author Simeon Andreev
 *
 */
public class TorManager extends Manager {

	/** The Tor working directory path. */
	private final Path workingDirectory;
	/** The managed Tor process. */
	private Process process = null;
	/** The thread reading the output of the Tor process. */
	private Thread output = null;
	/** Atomic boolean, true iff Tor bootstrapping is complete. */
	private AtomicBoolean ready = new AtomicBoolean(false);


	/**
	 * Constructor method.
	 *
	 * @throws IOException Propagates any IOException thrown by the temporary directory creation.
	 */
	public TorManager() throws IOException {
		super();
		String currentDirectory = System.getProperty(Constants.userdir);
		SimpleDateFormat formatter = new SimpleDateFormat(Constants.timestampformat);
		String timestamp = formatter.format(new Date());
		// Create temp directory for Tor to run in.
		workingDirectory = Files.createTempDirectory(Paths.get(currentDirectory), timestamp);
		logger.log(Level.INFO, "Created Tor working directory: " + workingDirectory.toString());
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

			/** The parameters for the Tor execution command. */
			final String[] parameters = {
				/** The Tor executable file. */
				Constants.torfile,
				/** The Tor configuration file option. */
				Constants.torrcoption,
				/** The Tor configuration file. */
				Constants.torrcfile,
				/** The Tor working directory option. */
				Constants.datadiroption,
				/** The Tor working directory path. */
				workingDirectory.toString(),
				/** The Tor control port output file option. */
				Constants.ctlportoutoption,
				/** The Tor control port output file path. */
				Paths.get(workingDirectory.toString(), Constants.portfile).toString()
			};

			logger.log(Level.INFO, "Executing Tor.");
			logger.log(Level.INFO, "Command: "
										// The Tor binary.
										+ parameters[0] + " "
										// The torrc option and path.
										+ parameters[1] + " " + parameters[2] + " "
										// The working directory option and path.
										+ parameters[3] + " " + parameters[4] + " "
										// The control port output file option and path.
										+ parameters[5] + " " + parameters[6]
			);

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
							logger.log(Level.INFO, "Output thread read Tor output line:\n" + line);
							// If we read null we are done with the process output.
							if (line == null) break;
							// Otherwise, check if we read that the bootstrapping is complete.
							if (line.contains(Constants.torbootstrapdone))
								ready.set(true);
						}

						logger.log(Level.INFO, "Output thread closing output stream.");
						input.close();
						logger.log(Level.INFO, "Output thread closed output stream.");
					} catch (IOException e) {
						logger.log(Level.WARNING, "Output thread caught an IOException: " + e.getMessage());
					}
				}
			});

			output.start();
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

		ready.set(false);
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

		logger.log(Level.INFO, "Deleting Tor working directory.");
		final File directory = workingDirectory.toFile();
		for (File file : directory.listFiles()) {
			logger.log(Level.INFO, "Deleting file: " + file.getName());
			final boolean fileDeleted = file.delete();
			if (fileDeleted) logger.log(Level.WARNING, "Was unable to delete a file in the Tor working directory: " + file.getName());
		}
		final boolean deletedDirectory = directory.delete();
		if (!deletedDirectory) logger.log(Level.WARNING, "Was unable to delete the Tor working directory: " + workingDirectory.toString());
		logger.log(Level.INFO, "Deleting Tor working directory done.");
	}

	/**
	 * Returns the Tor working directory path.
	 *
	 * @return The Tor working directory path.
	 */
	public Path directory() {
		return workingDirectory;
	}

	/**
	 * Returns whether Tors bootstrapping is complete.
	 *
	 * @return true iff Tor is running and the bootstrapping is complete.
	 */
	public boolean ready() {
		return ready.get();
	}

}
