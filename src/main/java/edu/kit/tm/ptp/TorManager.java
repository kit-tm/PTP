package edu.kit.tm.ptp;

import edu.kit.tm.ptp.hiddenservice.LockFile;
import edu.kit.tm.ptp.hiddenservice.LockFileFactory;
import edu.kit.tm.ptp.utility.Constants;

import net.freehaven.tor.control.TorControlConnection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A manager for the personal Tor process of the API.
 *
 * @author Timon Hackenjos
 * @author Simeon Andreev
 *
 */
public class TorManager {

  /** A reglar expression, used to find numbers in a string. */
  private static final String regex = "[0-9]+";

  /** The Tor working directory path. */
  private final String workingDirectory;
  /** The TorManager running file. */
  private File portsFile;
  /** The TorManager lock file. */
  private File lockFile;
  /** The managed Tor process. */
  private Process process = null;
  /** The thread reading the stdout of the Tor process. */
  private Thread output = null;
  /** The thread reading the stderr of the Tor process. */
  private Thread error = null;
  /** The thread waiting for the Tor ports file. */
  private Thread ports = null;
  /** The control port number of the Tor process. */
  private int torControlPort;
  /** The SOCKS proxy port number of the Tor process. */
  private int torSocksProxyPort;

  private static final Logger logger = Logger.getLogger(TorManager.class.getName());
  private volatile boolean torRunning = false;
  private volatile boolean torBootstrapped = false;
  private volatile boolean portsFileWritten = false;
  private boolean dedicatedTorProcess; // Kill Tor process independent of other PTP instances

  private class OutputThread implements Runnable {
    @Override
    public void run() {
      try {
        logger.log(Level.INFO, "Output reading thread started.");
        logger.log(Level.INFO, "Fetching the process stdout stream.");
        BufferedReader standardOut =
            new BufferedReader(new InputStreamReader(process.getInputStream(), Constants.charset));

        int controlPort = -1;
        int socksPort = -1;
        boolean controlPortRead = false;
        boolean socksPortRead = false;

        logger.log(Level.INFO, "Output thread entering reading loop.");

        while (!Thread.interrupted()) {
          if (standardOut.ready()) {
            String line = standardOut.readLine();

            logger.log(Level.INFO, "Output thread read Tor output line:\n" + line);

            // If we read null we are done with the process output.
            if (line == null) {
              break;
            }

            // if not, check if we read that the bootstrapping is complete.
            if (line.contains(Constants.bootstrapdone) && controlPortRead && socksPortRead) {
              logger.log(Level.INFO, "Output thread Tor ports to TorManager ports file.");

              BufferedWriter writer = new BufferedWriter(
                  new OutputStreamWriter(new FileOutputStream(portsFile), Constants.charset));

              writer.write(controlPort + Constants.newline);
              writer.write(socksPort + Constants.newline);

              logger.log(Level.INFO, "Output thread wrote: " + controlPort);
              logger.log(Level.INFO, "Output thread wrote: " + socksPort);

              writer.close();
              logger.log(Level.INFO, "Output thread wrote TorManager ports file.");
              portsFileWritten = true;
              // If not, check whether the control port is open.
            } else if (line.contains(Constants.controlportopen)) {
              controlPort = readPort(Constants.controlportopen, line);
              controlPortRead = true;
              // If not, check whether the SOCKS proxy port is open.
            } else if (line.contains(Constants.socksportopen)) {
              socksPort = readPort(Constants.socksportopen, line);
              socksPortRead = true;
            }
          } else {
            try {
              // If the Tor process bootstrapping is not done, sleep less. Otherwise sleep for a
              // longer interval.
              if (torBootstrapped) {
                Thread.sleep(5 * 1000);
              } else {
                Thread.sleep(1 * 1000);
              }
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
          }
        }

        standardOut.close();
      } catch (IOException e) {
        logger.log(Level.WARNING, "Output thread caught an IOException: " + e.getMessage());
      }
    }
  }

  private class ErrorThread implements Runnable {
    @Override
    public void run() {
      try {
        logger.log(Level.INFO, "Error reading thread started.");
        logger.log(Level.INFO, "Fetching the process stderr stream.");

        BufferedReader errorOut =
            new BufferedReader(new InputStreamReader(process.getErrorStream(), Constants.charset));

        logger.log(Level.INFO, "Error thread entering reading loop.");

        while (!Thread.interrupted()) {
          if (errorOut.ready()) {
            String line = errorOut.readLine();

            logger.log(Level.INFO, "Error thread read Tor output line:\n" + line);

            // If we read null we are done with the process output.
            if (line == null) {
              break;
            }
          } else {
            try {
              Thread.sleep(10 * 1000);
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
          }
        }

        errorOut.close();
      } catch (IOException e) {
        logger.log(Level.WARNING, "Error thread caught an IOException: " + e.getMessage());
      }
    }
  }

  private class PortsThread implements Runnable {
    @Override
    public void run() {
      logger.log(Level.INFO, "TorManager ports thread entering execution loop.");
      logger.log(Level.INFO, "TorManager ports thread sleeping.");

      while (!portsFileWritten) {
        try {
          Thread.sleep(2 * 1000);
        } catch (InterruptedException e) {
          return;
        }
      }

      logger.log(Level.INFO, "TorManager ports thread done waiting.");
      logger.log(Level.INFO, "TorManager ports thread reading ports file.");

      try {
        // Read the control and SOCKS ports from the Tor ports file.
        readPorts();

        torBootstrapped = true;
        logger.log(Level.INFO, "Tor bootstrapping was successfull.");
      } catch (IOException e) {
        logger.log(Level.WARNING,
            "Ports thread caught an IOException while attempting to read the ports file: "
                + e.getMessage());
      }

      logger.log(Level.INFO, "Ports thread exiting execution loop.");
    }
  }

  /**
   * Constructs a new TorManager object.
   * 
   * @param workingDirectory The directory to run Tor in.
   */
  public TorManager(String workingDirectory) {
    this(workingDirectory, false);
  }

  /**
   * Constructs a new TorManager object.
   *
   * @param workingDirectory The directory to run Tor in,
   * @param dedicatedTorProcess If true the TorManager will kill the Tor process on exit
   *                         without checking if it's used by others.
   */
  public TorManager(String workingDirectory, boolean dedicatedTorProcess) {
    this.workingDirectory = workingDirectory;
    this.dedicatedTorProcess = dedicatedTorProcess;
  }

  /**
   * Checks if a Tor process is already running and otherwise starts a new Tor process.
   */
  public void startTor() throws IOException {
    // Check if the home directory exists, if not try to create it.
    File directory = new File(workingDirectory);
    if (!directory.exists() && !directory.mkdirs()) {
      // Home directory does not exist and was not created.
      throw new IOException("Unable to create missing PTP home directory.");
    }

    logger.log(Level.INFO, "TorManager working directory exists.");

    // Check if the lock file exists, if not create it.
    lockFile = new File(workingDirectory + File.separator + Constants.tormanagerlockfile);
    if (!lockFile.exists() && !lockFile.createNewFile()) {
      // Lock file does not exist and was not created.
      throw new IOException("Unable to create missing PTP TorManager lock file.");
    }

    // Get a handle on the TorManager ports file.
    portsFile = new File(workingDirectory + File.separator + Constants.tormanagerportsfile);

    logger.log(Level.INFO, "Read Tor working directory: " + workingDirectory.toString());
    logger.log(Level.INFO, "TorManager lock file is: " + lockFile.getAbsolutePath());
    logger.log(Level.INFO, "TorManager ports file is: " + portsFile.getAbsolutePath());

    // Check if another TorManager is currently running a Tor process.
    LockFile lock = null;
    try {
      // Block until a lock on the TorManager lock file is available.
      logger.log(Level.INFO, "TorManager acquiring lock on TorManager lock file.");

      lock = LockFileFactory.getLockFile(lockFile);
      lock.lock();
      RandomAccessFile raf = lock.getRandomAccessFile();

      logger.log(Level.INFO, "TorManager has the lock on the TorManager lock file.");

      if (portsFile.exists()) {
        logger.log(Level.INFO, "TorManager checking if Tor is running.");

        readPorts();
        // Attempt a JTorCtl connection to the Tor process. If Tor is not running the connection
        // will not succeed.
        try {
          logger.log(Level.INFO, "TorManager attempting to connect to the Tor process.");

          Socket socket = new Socket(Constants.localhost, torControlPort);
          TorControlConnection conn = new TorControlConnection(socket);
          conn.authenticate(new byte[0]);
          socket.close();

          torRunning = true;
          torBootstrapped = true;
        } catch (IOException e) {
          logger.log(Level.INFO, "TorManager could not connect to the Tor process,"
              + "the file lock counter is broken.");
        }
      }
      int numApis = raf.length() == 0 ? 0 : raf.readInt();
      logger.log(Level.INFO,
          "TorManager checked if Tor is running: " + (torRunning ? "running" : "not running"));

      if (!torRunning) {
        // Run the Tor process.
        final boolean success = runTor();
        // Check if the Tor process failed to start.
        if (success) {
          torRunning = true;
          raf.seek(0);
          raf.writeInt(1);

          logger.log(Level.INFO, "TorManager set lock file counter to: " + 1);
        } else {
          throw new IOException("Failed to start Tor");
        }
        // Otherwise, increment the counter in the lock file. Indicates that another API is using
        // the Tor process.
      } else {
        raf.seek(0);
        raf.writeInt(numApis + 1);

        logger.log(Level.INFO, "TorManager set lock file counter to: " + (numApis + 1));
      }

      // Release the lock.
      logger.log(Level.INFO, "TorManager releasing the lock on the TorManager lock file.");
      lock.release();
      lock = null;
    } finally {
      if (lock != null) {
        lock.release();
      }
    }
  }

  /**
   * Waits until the running Tor process finished bootstrapping.
   * 
   * @param timeout Time in millisecods to wait for the bootstrapping to finish.
   */
  public void waitForBootstrapping(long timeout) throws InterruptedException {
    long start = System.currentTimeMillis();

    while (torRunning && !torBootstrapped && System.currentTimeMillis() - start < timeout) {
      Thread.sleep(1 * 1000);
    }
  }

  /**
   * Stops the Tor process if it isn't used by others.
   */
  public void stopTor() {
    logger.log(Level.INFO, "Closing output thread.");
    if (output != null) {
      output.interrupt();
      try {
        output.join();
      } catch (InterruptedException e) {
        logger.log(Level.INFO, "Got interrupted while waiting for the output thread.");
      }
    }

    logger.log(Level.INFO, "Closing error thread.");
    if (error != null) {
      error.interrupt();
      try {
        error.join();
      } catch (InterruptedException e) {
        logger.log(Level.INFO, "Got interrupted while waiting for the error thread.");
      }
    }

    logger.log(Level.INFO, "Interrupting ports thread.");
    if (ports != null) {
      ports.interrupt();
      try {
        ports.join();
      } catch (InterruptedException e) {
        logger.log(Level.INFO, "Got interrupted while waiting for the ports thread.");
      }
    }

    // Stop Tor, if no other API is using the process.
    shutdownTor();

    torRunning = false;
    torBootstrapped = false;
  }

  /**
   * Returns the control port number of the Tor process.
   */
  public int getTorControlPort() {
    if (!torBootstrapped) {
      throw new IllegalStateException("Bootstrapping not done!");
    }

    return torControlPort;
  }

  /**
   * Returns the SOCKS proxy port number of the Tor process.
   */
  public int getTorSOCKSPort() {
    if (!torBootstrapped) {
      throw new IllegalStateException("Bootstrapping not done!");
    }

    return torSocksProxyPort;
  }

  /**
   * Returns true if a Tor process is running.
   */
  public boolean torRunning() {
    return torRunning;
  }

  /**
   * Returns true if the Tor process finished bootstrapping.
   */
  public boolean torBootstrapped() {
    return torBootstrapped;
  }

  /**
   * Returns the Tor working directory path.
   */
  public String getTorWorkingDirectory() {
    return workingDirectory;
  }


  /**
   * Stops the Tor process, if no other API is using the process.
   */
  private void shutdownTor() {
    if (!torRunning) {
      return;
    }

    logger.log(Level.INFO, "TorManager checking if Tor process should be closed.");
    LockFile lock = null;

    try {
      logger.log(Level.INFO, "TorManager acquiring lock on TorManager lock file.");
      lock = LockFileFactory.getLockFile(lockFile);
      lock.lock();
      RandomAccessFile raf = lock.getRandomAccessFile();

      logger.log(Level.INFO, "TorManager has the lock on the TorManager lock file.");

      final long length = raf.length();

      // If the lock file does not contain a single integer, something is wrong.
      if (length == Integer.SIZE / 8) {

        final int numberOfApis = raf.readInt();
        logger.log(Level.INFO,
            "TorManager read the number of APIs from the TorManager lock file: " + numberOfApis);

        raf.seek(0);
        raf.writeInt(Math.max(0, numberOfApis - 1));

        // Check if this is the only API using the Tor process, if so stop the Tor process.
        if (numberOfApis == 1 || dedicatedTorProcess) {
          logger.log(Level.INFO, "TorManager stopping Tor process.");
          if (process != null) {
            logger.log(Level.INFO, "Killing own Tor process");
            process.destroy();
          } else {
            logger.log(Level.INFO, "Using control port: " + torControlPort);

            Socket socket = new Socket(Constants.localhost, torControlPort);
            logger.log(Level.INFO, "TorManager attempting to shutdown Tor process.");

            TorControlConnection conn = new TorControlConnection(socket);
            conn.authenticate(new byte[0]);
            conn.shutdownTor(Constants.shutdownsignal);
            socket.close();

            logger.log(Level.INFO, "TorManager sent shutdown signal.");
          }

          // Delete the TorManager ports file.
          if (portsFile.delete()) {
            logger.log(Level.INFO, "Deleted TorManager ports file.");
          } else {
            logger.log(Level.WARNING, "Failed to delete ports file "
                + portsFile.getAbsolutePath());
          }
        }
      } else {
        logger.log(Level.WARNING, "TorManager file lock is broken!");
      }

      // Release the lock.
      logger.log(Level.INFO, "TorManager releasing the lock on the TorManager lock file.");
      lock.release();
      lock = null;
    } catch (IOException e) {
      logger.log(Level.WARNING,
          "TorManager caught an IOException while closing Tor process: " + e.getMessage());
    } finally {
      if (lock != null) {
        lock.release();
      }
    }
  }

  /**
   * Starts a Tor process and creates a separate thread to read the Tor logging output.
   *
   * @return True, if the Tor process started successfully.
   */
  private boolean runTor() {
    logger.log(Level.INFO, "TorManager starting Tor process.");

    boolean useAbsolutePath = workingDirectory != null
        && new File(workingDirectory + File.separator + Constants.torfile).exists();

    String torFile = useAbsolutePath ? workingDirectory + File.separator : "";
    torFile += Constants.torfile;
    String torrcFile = useAbsolutePath ? workingDirectory + File.separator : "";
    torrcFile += Constants.torrcfile;

    try {
      /** The parameters for the Tor execution command. */
      final String[] parameters = {
          /** The Tor executable file. */
          torFile,
          /** The Tor configuration file option. */
          Constants.torrcoption,
          /** The Tor configuration file. */
          torrcFile,
          /** The Tor working directory option. */
          Constants.datadiroption,
          /** The Tor working directory path. */
          workingDirectory};

      logger.log(Level.INFO, "Executing Tor.");
      logger.log(Level.INFO,
          "Command: "
              // The Tor binary.
              + parameters[0] + " "
              // The torrc option and path.
              + parameters[1] + " " + parameters[2] + " "
              // The working directory option and path.
              + parameters[3] + " " + parameters[4]);

      process = Runtime.getRuntime().exec(parameters);

      error = new Thread(new ErrorThread());
      output = new Thread(new OutputThread());
      ports = new Thread(new PortsThread());

      // Start the log output reading thread.
      output.start();
      error.start();
      ports.start();
    } catch (IOException e) {
      logger.log(Level.WARNING,
          "TorManager thread caught an IOException when starting Tor: " + e.getMessage());
      return false;
    }

    return true;
  }

  /**
   * Reads the Tor control and SOCKS ports from the TorManager ports file.
   *
   * @throws IOException Propagates any IOException thrown when reading the TorManager ports file.
   */
  private void readPorts() throws IOException {
    BufferedReader reader = new BufferedReader(
        new InputStreamReader(new FileInputStream(portsFile), Constants.charset));

    String controlPortLine = reader.readLine();
    String socksPortLine = reader.readLine();

    try {
      torControlPort = Integer.parseInt(controlPortLine);
      torSocksProxyPort = Integer.parseInt(socksPortLine);
    } catch (NumberFormatException e) {
      throw new IOException("Failed to read ports file.");
    } finally {
      reader.close();
    }

    logger.log(Level.INFO, "Ports thread read Tor control port from file: " + torControlPort);
    logger.log(Level.INFO, "Ports thread read Tor SOCKS port from file: " + torSocksProxyPort);
  }

  /**
   * Reads a port number from a Tor logging output line.
   *
   * @param prefix The prefix of where the actual line (without time-stamp and so on) starts.
   * @param line The Tor logging line.
   * @return The port number contained in the logging line.
   */
  private int readPort(String prefix, String line) {
    // Get the start of the actual output, without the time, date and so on.
    int position = line.indexOf(prefix);
    // Get the output substring.
    String output = line.substring(position);
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(output);

    if (!matcher.find()) {
      throw new IllegalArgumentException("Port number not found in line: " + line);
    }

    String port = matcher.group();

    logger.log(Level.INFO, "Read port: " + port + " [" + line + "].");
    return Integer.parseInt(port);
  }
}
