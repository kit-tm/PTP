package thread;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.freehaven.tor.control.TorControlConnection;
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

		// Read the TorP2P home directory from the system environment variable.
		// or use default if not set.
		String torp2phome = System.getenv(Constants.torp2phomeenvvar);
		if(torp2phome == null) torp2phome = Constants.torp2phomedefault;
		workingDirectory = Paths.get(torp2phome);

		// Check if the home directory exists, if not try to create it.
		File directory = workingDirectory.toFile();
		if (!directory.exists() && !directory.mkdirs())
			// Home directory does not exist and was not created.
			throw new IOException("Unable to create missing TorP2P home directory.");

		// Check if the lock file exists, if not create it.
		lockFile = Paths.get(workingDirectory.toString(), Constants.tormanagerlockfile).toFile();
		if (!lockFile.exists() && !lockFile.createNewFile())
			// Lock file does not exist and was not created.
			throw new IOException("Unable to create missing TorP2P Tor manager lock file.");

		// Get a handle on the Tor manager ports file.
		portsFile = Paths.get(workingDirectory.toString(), Constants.tormanagerportsfile).toFile();

		// The Tor manager ports file should be deleted on exit.
		portsFile.deleteOnExit();

		logger.log(Level.INFO, "Read Tor working directory: " + workingDirectory.toString());
		logger.log(Level.INFO, "Tor manager lock file is: " + lockFile.getAbsolutePath());
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
				FileLock lock = null;

				// Lock the Tor manager lock file.
				try {
					lock = channel.tryLock();
					if (lock == null)
						throw new OverlappingFileLockException();

					logger.log(Level.INFO, "Tor manager has the lock on the Tor manager lock file.");
					boolean noTor = raf.length() == 0 || raf.readInt() <= 0;
					logger.log(Level.INFO, "Tor manager checked if Tor process is running: " + (noTor ? "not running" : "running"));

					// If the lock file is empty, or its content is the number 0, no Tor process is running.
					if (noTor) {
						// Run the Tor process.
						runtor();
						// Set the number of APIs using Tor to one.
						raf.seek(0);
						raf.writeInt(1);
					// Otherwise, increment the counter in the lock file. Indicates that another API is using the Tor process.
					} else {
						raf.seek(0);
						final int numberOfAPIs = raf.readInt();
						raf.seek(0);
						raf.writeInt(numberOfAPIs + 1);
					}

				} catch (OverlappingFileLockException e) {
					logger.log(Level.INFO, "Tor manager caught an OverlappingFileLockException while locking file, backing off.");
				} finally {
					// Release the lock.
					logger.log(Level.INFO, "Tor manager releasing the lock on the Tor manager lock file.");
					if (lock != null)
						lock.release();

					// Close the lock file.
					channel.close();
					raf.close();
				}

				// Wait for the Tor manager ports file to appear and read it.
				waittor();
			} catch (IOException e) {
				e.printStackTrace();
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
		logger.log(Level.INFO, "Interrupting Tor manager thread.");
		thread.interrupt();
		logger.log(Level.INFO, "Closing output thread.");
		if (output != null) output.interrupt();
		logger.log(Level.INFO, "Closing error thread.");
		if (error != null) error.interrupt();
		logger.log(Level.INFO, "Interrupting ports thread.");
		if (ports != null) ports.interrupt();
		logger.log(Level.INFO, "Stopped Tor manager thread.");

		// Stop Tor, if no other API is using the process.
		if (running.get())
			stoptor();

		running.set(false);
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

	public boolean torrunning() {
		boolean torRunning = false;

		logger.log(Level.INFO, "Tor manager checking if Tor is running.");
		File directory = workingDirectory.toFile();
		if (running.get() && directory.exists()) {
			// Check if another TorManager is currently running a Tor process.
			try {
				// Block until a lock on the Tor manager lock file is available.
				logger.log(Level.INFO, "Tor manager acquiring lock on Tor manager lock file.");
				RandomAccessFile raf = new RandomAccessFile(lockFile, Constants.readwriterights);
				FileChannel channel = raf.getChannel();
				FileLock lock = null;

				// Lock the Tor manager lock file.
				try {
					lock = channel.tryLock();
					if (lock == null)
						throw new OverlappingFileLockException();

					logger.log(Level.INFO, "Tor manager has the lock on the Tor manager lock file.");

					torRunning = raf.length() == Integer.BYTES && raf.readInt() > 0;
					// If the lock file is empty, or its content is the number 0, no Tor process is running.
				} catch (OverlappingFileLockException e) {
					logger.log(Level.INFO, "Tor manager caught an OverlappingFileLockException while locking file, backing off.");
				} finally {
					// Release the lock.
					logger.log(Level.INFO, "Tor manager releasing the lock on the Tor manager lock file.");
					if (lock != null)
						lock.release();

					// Close the lock file.
					channel.close();
					raf.close();
				}
			} catch (IOException e) {
				logger.log(Level.WARNING, "Tor manager caught an IOException during Tor process initialization: " + e.getMessage());
			}
		}

		return torRunning;
	}


	/**
	 * Stops the API Tor process, if no other API is using the process.
	 */
	private void stoptor() {
		logger.log(Level.INFO, "Tor manager closing Tor process.");
		try {
			logger.log(Level.INFO, "Tor manager acquiring lock on Tor manager lock file.");
			RandomAccessFile raf = new RandomAccessFile(lockFile, Constants.readwriterights);
			FileChannel channel = raf.getChannel();
			FileLock lock = null;

			// Lock the Tor manager lock file.
			try {
				lock = channel.tryLock();
				if (lock == null)
					throw new OverlappingFileLockException();

				logger.log(Level.INFO, "Tor manager has the lock on the Tor manager lock file.");

				// If the lock file does not contain a single integer, something is wrong.
				if (raf.length() != Integer.BYTES)
					throw new IOException("Tor manager lock file is broken!");

				final int numberOfAPIs = raf.readInt();
				logger.log(Level.INFO, "Tor manager read the number of APIs from the Tor manager lock file: " + numberOfAPIs);

				raf.seek(0);
				raf.writeInt(Math.max(0, numberOfAPIs - 1));

				// Check if this is the only API using the Tor process, if so stop the Tor process.
				if (numberOfAPIs == 1) {
					logger.log(Level.INFO, "Tor manager stopping Tor process.");
					logger.log(Level.INFO, "Using control port: " + torControlPort);
					Socket s = new Socket(Constants.localhost, torControlPort);
					logger.log(Level.INFO, "Tor manager attempting to shutdown Tor process.");
					TorControlConnection conn = TorControlConnection.getConnection(s);
					conn.authenticate(new byte[0]);
					conn.shutdownTor(Constants.shutdownsignal);
					logger.log(Level.INFO, "Tor manager sent shutdown signal.");
					// Delete the Tor manager ports file.
					portsFile.delete();
					logger.log(Level.INFO, "Deleted Tor manager ports file.");
				}
			} catch (OverlappingFileLockException e) {
				logger.log(Level.INFO, "Tor manager caught an OverlappingFileLockException while locking file, backing off.");
			} finally {
				// Release the lock.
				logger.log(Level.INFO, "Tor manager releasing the lock on the Tor manager lock file.");
				if (lock != null)
					lock.release();

				// Close the lock file.
				channel.close();
				raf.close();
			}
		} catch (IOException e) {
			logger.log(Level.WARNING, "Tor manager caught an IOException while closing Tor process: " + e.getMessage());
		} catch (NumberFormatException e) {
			logger.log(Level.WARNING, "Tor manager caught a NumberFormatException while closing Tor process: " + e.getMessage());
		}
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
					// Read the control and SOCKS ports from the Tor ports file.
					readports();

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
						BufferedReader errorOut = new BufferedReader(new InputStreamReader(process.getErrorStream()));

						logger.log(Level.INFO, "Error thread entering reading loop.");
						while (condition.get()) {
							if (errorOut.ready()) {
								String line = errorOut.readLine();
								logger.log(Level.INFO, "Error thread read Tor output line:\n" + line);
								// If we read null we are done with the process output.
								if (line == null) break;
							} else {
								try {
									Thread.sleep(10 * 1000);
								} catch (InterruptedException e) {
									// Waiting was interrupted. Do nothing.
								}
							}
						}

						errorOut.close();
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
						BufferedReader standardOut = new BufferedReader(new InputStreamReader(process.getInputStream()));
						boolean portsRead = false;

						logger.log(Level.INFO, "Output thread entering reading loop.");
						while (condition.get()) {
							if (standardOut.ready()) {
								String line = standardOut.readLine();
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

									portsRead = true;
								// If not, check whether the control port is open.
								} else if (line.contains(Constants.controlportopen))
									torControlPort = readport(Constants.controlportopen, line);
								// If not, check whether the SOCKS proxy port is open.
								else if (line.contains(Constants.socksportopen))
									torSOCKSProxyPort = readport(Constants.socksportopen, line);
							} else {
								try {
									// If the Tor process ports are not read, sleep less. Otherwise sleep for a longer interval.
									if (portsRead)
										Thread.sleep(5 * 1000);
									else
										Thread.sleep(1 * 1000);
								} catch (InterruptedException e) {
									// Waiting was interrupted. Do nothing.
								}
							}
						}

						standardOut.close();
					} catch (IOException e) {
						logger.log(Level.WARNING, "Output thread caught an IOException: " + e.getMessage());
					}
				}
			});

			// Start the log output reading thread.
			output.start();
			error.start();
		} catch (IOException e) {
			logger.log(Level.WARNING, "Tor manager thread caught an IOException when starting Tor: " + e.getMessage());
		}
	}

	/**
	 * Reads the Tor control and SOCKS ports from the Tor manager ports file.
	 *
	 * @throws IOException Propagates any IOException thrown when reading the Tor manager ports file.
	 */
	private void readports() throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(portsFile));

		String controlPortLine = reader.readLine();
		String socksPortLine = reader.readLine();

		torControlPort = Integer.valueOf(controlPortLine).intValue();
		torSOCKSProxyPort = Integer.valueOf(socksPortLine).intValue();

		reader.close();

		logger.log(Level.INFO, "Ports thread read Tor control port from file: " + torControlPort);
		logger.log(Level.INFO, "Ports thread r read Tor SOCKS port from file: " + torSOCKSProxyPort);
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
