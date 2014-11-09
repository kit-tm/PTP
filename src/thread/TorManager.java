package thread;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import p2p.Constants;


/**
 * A manager for the personal Tor process of the API.
 *
 * @author Simeon Andreev
 *
 */
public class TorManager extends Manager {

	/** A reglar expression, used to find numbers in a string. */
	private static final String regex = "[0-9]+";

	/** The Tor working directory path. */
	private final Path workingDirectory;
	/** The Tor manager ports file. */
	private final File runningFile;
	/** The Tor manager running file. */
	private final File portsFile;
	/** The Tor manager lock file. */
	private final File lockFile;
	/** The managed Tor process. */
	private Process process = null;
	/** The thread reading the stdout of the Tor process. */
	private Thread output = null;
	/** The thread reading the stderr of the Tor process. */
	private Thread error = null;
	/** The thread waiting for the Tor ports file. */
	private Thread ports = null;
	/** Atomic boolean, true iff Tor bootstrapping is complete. */
	private AtomicBoolean ready = new AtomicBoolean(false);
	/** The control port number of the Tor process. */
	private int torControlPort;
	/** The SOCKS proxy port number of the Tor process. */
	private int torSOCKSProxyPort;


	/**
	 * Constructor method.
	 *
	 * @throws IOException Propagates any IOException thrown by the temporary directory creation.
	 */
	public TorManager() throws IOException {
		super();

		// Read the TorP2P home directory from the system environment variables.
		String torp2phome = System.getenv(Constants.torp2phomeenvvar);
		workingDirectory = Paths.get(torp2phome);

		// Check if the home directory exists, if not try to create it.
		File directory = workingDirectory.toFile();
		if (!directory.exists() && !directory.mkdirs())
			// Home directory does not exist and was not created.
			throw new IOException("Unable to create missing TorP2P home directory.");

		// Check if the lock file exists, if not create it.
		lockFile = Paths.get(workingDirectory.toString(), Constants.tormanagerlockfile).toFile();
		if (!lockFile.exists())
			lockFile.createNewFile();

		// Get handles of the auxiliary files.
		runningFile = Paths.get(workingDirectory.toString(), Constants.tormanagerrunningfile).toFile();
		portsFile = Paths.get(workingDirectory.toString(), Constants.tormanagerportsfile).toFile();

		// The Tor manager auxiliary files should always be deleted on exit.
		runningFile.deleteOnExit();
		portsFile.deleteOnExit();

		logger.log(Level.INFO, "Read Tor working directory: " + workingDirectory.toString());
		logger.log(Level.INFO, "Created Tor manager lock file: " + lockFile.getAbsolutePath());
		logger.log(Level.INFO, "Tor manager running file is: " + runningFile.getAbsolutePath());
		logger.log(Level.INFO, "Tor manager ports file is: " + portsFile.getAbsolutePath());
		logger.log(Level.INFO, "TorManager object created.");
	}


	/**
	 * @see Runnable
	 */
	@Override
	public void run() {
		logger.log(Level.INFO, "Tor manager thread started.");
		running.set(true);

		// Check if the working directory still exists, if not do nothing.
		File directory = workingDirectory.toFile();
		if (directory.exists()) {
			logger.log(Level.INFO, "Tor manager working directory exists.");
			// Check if another TorManager is currently running a Tor process.
			try {
				// Block until a lock on the Tor manager lock file is available.
				logger.log(Level.INFO, "Tor manager acquiring lock on Tor manager lock file.");
				RandomAccessFile raf = new RandomAccessFile(lockFile, Constants.readwriterights);
				FileChannel channel = raf.getChannel();
				FileLock lock = channel.lock();

				logger.log(Level.INFO, "Tor manager has the lock on the Tor manager lock file.");

				final boolean running = runningFile.exists();
				logger.log(Level.INFO, "Tor manager checked if Tor is running: " + (running ? "yes" : "no"));
				final boolean created = !running && runningFile.createNewFile();
				logger.log(Level.INFO, "Tor manager created Tor manager running file: " + (created ? "yes" : "no"));

				// Release the lock.
				logger.log(Level.INFO, "Tor manager releasing the lock on the Tor manager lock file.");
				lock.release();

				// Check if a Tor process is already running, if so its Tor manager created the Tor manager running file.
				if (running)
					waittor();
				// Otherwise run Tor with this Tor manager.
				else if (created)
					runtor();
				else
					logger.log(Level.WARNING, "Tor manager was unable to create the Tor manager running file!");

				// Close the lock file.
				channel.close();
				raf.close();
			} catch (IOException e) {
				logger.log(Level.WARNING, "Tor manager caught an IOException during Tor process initialization: " + e.getMessage());
			}
		}

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
		if (process != null) process.destroy();
		logger.log(Level.INFO, "Interrupting Tor manager thread.");
		thread.interrupt();
		logger.log(Level.INFO, "Interrupting output thread.");
		if (output != null) output.interrupt();
		logger.log(Level.INFO, "Interrupting error thread.");
		if (error != null) error.interrupt();
		logger.log(Level.INFO, "Interrupting ports thread.");
		if (ports != null) ports.interrupt();
		logger.log(Level.INFO, "Stopped Tor manager thread.");

		// Wait for the process thread to exit.
		logger.log(Level.INFO, "Waiting for TorManager thread to exit...");
		while (thread.isAlive()) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				// Do nothing.
			}
		}
		logger.log(Level.INFO, "Tor manager thread exited.");

		// Delete the Tor manager ports file.
		portsFile.delete();
		logger.log(Level.INFO, "Deleted Tor manager ports file.");

		// Delete the Tor manager running file.
		runningFile.delete();
		logger.log(Level.INFO, "Deleted Tor manager running file.");
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

	/**
	 * Returns the control port number of the Tor process.
	 *
	 * @return The control port number of the Tor process.
	 * @throws IllegalArgumentException Throws an IllegalArgumentException if the boostrapping of the Tor process is not yet done.
	 */
	public int controlport() {
		if (!ready.get())
			throw new IllegalArgumentException("Bootstrapping not done!");

		return torControlPort;
	}

	/**
	 * Returns the SOCKS proxy port number of the Tor process.
	 *
	 * @return The SOCKS proxy port number of the Tor process.
	 * @throws IllegalArgumentException Throws an IllegalArgumentException if the boostrapping of the Tor process is not yet done.
	 */
	public int socksport() {
		if (!ready.get())
			throw new IllegalArgumentException("Bootstrapping not done!");

		return torSOCKSProxyPort;
	}


	/**
	 * Starts a thread which waits for the Tor ports file to appear, then reads the control/SOCKS ports from the file.
	 */
	private void waittor() {
		logger.log(Level.INFO, "Tor manager waiting on already running Tor process.");
		ports = new Thread(new Runnable() {

			@Override
			public void run() {
				logger.log(Level.INFO, "Tor manager ports thread entering execution loop.");

				logger.log(Level.INFO, "Tor manager ports thread sleeping.");
				while (!portsFile.exists() && running.get()) {
					try {
						// TODO: parameter for this
						Thread.sleep(2 * 1000);
					} catch (InterruptedException e) {
						// Waiting was interrupted, do nothing.
					}
				}

				logger.log(Level.INFO, "Tor manager ports thread done waiting.");
				// Check if the Tor Manager is still running.
				if (!running.get())
					return;

				logger.log(Level.INFO, "Tor manager ports thread reading ports file.");
				try {
					BufferedReader reader = new BufferedReader(new FileReader(portsFile));

					String controlPortLine = reader.readLine();
					String socksPortLine = reader.readLine();

					torControlPort = Integer.valueOf(controlPortLine).intValue();
					torSOCKSProxyPort = Integer.valueOf(socksPortLine).intValue();

					reader.close();

					logger.log(Level.INFO, "Ports thread read Tor control port from file: " + torControlPort);
					logger.log(Level.INFO, "Ports thread r read Tor SOCKS port from file: " + torSOCKSProxyPort);

					ready.set(true);
					logger.log(Level.INFO, "Tor manager ready.");
				} catch (IOException e) {
					logger.log(Level.WARNING, "Ports thread caught an IOException while attempting to read the ports file: " + e.getMessage());
					running.set(false);
				}

				logger.log(Level.INFO, "Ports thread exiting execution loop.");
			}



		});

		ports.start();
		logger.log(Level.INFO, "Ports thread started.");
	}

	/**
	 * Starts a Tor process and creates a separate thread to read the Tor logging output.
	 */
	private void runtor() {
		logger.log(Level.INFO, "Tor manager starting Tor process.");

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
				workingDirectory.toString()
			};

			logger.log(Level.INFO, "Executing Tor.");
			logger.log(Level.INFO, "Command: "
										// The Tor binary.
										+ parameters[0] + " "
										// The torrc option and path.
										+ parameters[1] + " " + parameters[2] + " "
										// The working directory option and path.
										+ parameters[3] + " " + parameters[4]
			);

			process = Runtime.getRuntime().exec(parameters);
			error = new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						logger.log(Level.INFO, "Error reading thread started.");
						logger.log(Level.INFO, "Fetching the process stdin stream.");
						final BufferedReader input = new BufferedReader(new InputStreamReader(process.getErrorStream()));

						logger.log(Level.INFO, "Error thread entering reading loop.");
						while (condition.get()) {
							logger.log(Level.INFO, "Error thread waiting on Tor output.");
							String line = input.readLine();
							logger.log(Level.INFO, "Error thread read Tor output line:\n" + line);
							// If we read null we are done with the process output.
							if (line == null) break;
						}

						logger.log(Level.INFO, "Error thread closing output stream.");
						input.close();
						logger.log(Level.INFO, "Error thread closed output stream.");
					} catch (IOException e) {
						logger.log(Level.WARNING, "Error thread caught an IOException: " + e.getMessage());
					}
				}

			});
			output = new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						logger.log(Level.INFO, "Output reading thread started.");
						logger.log(Level.INFO, "Fetching the process stdout stream.");
						final BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));

						logger.log(Level.INFO, "Output thread entering reading loop.");
						while (condition.get()) {
							logger.log(Level.INFO, "Output thread waiting on Tor output.");
							String line = input.readLine();
							logger.log(Level.INFO, "Output thread read Tor output line:\n" + line);
							// If we read null we are done with the process output.
							if (line == null) break;
							// If not, check if we read that the bootstrapping is complete.
							if (line.contains(Constants.bootstrapdone)) {
								logger.log(Level.INFO, "Output thread Tor ports to Tor manager ports file.");
								BufferedWriter writer = new BufferedWriter(new FileWriter(portsFile));

								writer.write(torControlPort + Constants.newline);
								writer.write(torSOCKSProxyPort + Constants.newline);

								logger.log(Level.INFO, "Output thread writing wrote: " + torControlPort);
								logger.log(Level.INFO, "Output thread writing wrote: " + torSOCKSProxyPort);

								writer.close();
								logger.log(Level.INFO, "Output thread wrote Tor manager ports file.");

								ready.set(true);
								logger.log(Level.INFO, "Tor manager ready.");
							// If not, check whether the control port is open.
							} else if (line.contains(Constants.controlportopen))
								torControlPort = readport(Constants.controlportopen, line);
							// If not, check whether the SOCKS proxy port is open.
							else if (line.contains(Constants.socksportopen))
								torSOCKSProxyPort = readport(Constants.socksportopen, line);
						}

						logger.log(Level.INFO, "Output thread closing output stream.");
						input.close();
						logger.log(Level.INFO, "Output thread closed output stream.");
					} catch (IOException e) {
						logger.log(Level.WARNING, "Output thread caught an IOException: " + e.getMessage());
					}
				}
			});

			// Start the log output reading thread.
			output.start();
			error.start();
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
		logger.log(Level.INFO, "Tor manager Tor process exited.");
	}

	/**
	 * Reads a port number from a Tor logging output line.
	 *
	 * @param prefix The prefix of where the actual line (without time-stamp and so on) starts.
	 * @param line The Tor logging line.
	 * @return The port number contained in the logging line.
	 */
	private int readport(String prefix, String line) {
		// Get the start of the actual output, without the time, date and so on.
		int position = line.indexOf(prefix);
		// Get the output substring.
		String output = line.substring(position);
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(output);

		if (!matcher.find())
			throw new IllegalArgumentException("Port number not found in line: " + line);

		String port = matcher.group();

		logger.log(Level.INFO, "Read port: " + port + " [" + line + "].");
		return Integer.valueOf(port);
	}

}
