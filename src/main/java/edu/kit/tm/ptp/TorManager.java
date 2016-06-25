package edu.kit.tm.ptp;

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
import java.net.Socket;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A manager for the Tor process of the API.
 *
 * @author Timon Hackenjos
 * @author Simeon Andreev
 *
 */
public class TorManager {

  /** A reglar expression, used to find numbers in a string. */
  private static final String regex = "[0-9]+";
  /** The Tor working directory path. */
  protected final String workingDirectory;
  /** The TorManager running file. */
  private File portsFile;
  /** The managed Tor process. */
  protected Process process = null;
  /** The thread reading the stdout of the Tor process. */
  private Thread output = null;
  /** The thread reading the stderr of the Tor process. */
  private Thread error = null;
  /** The thread waiting for the Tor ports file. */
  private Thread ports = null;
  /** The control port number of the Tor process. */
  private volatile int torControlPort = -1;
  /** The SOCKS proxy port number of the Tor process. */
  private volatile int torSocksProxyPort;
  private TorControlConnection controlConn;
  private Socket controlSocket;

  private static final Logger logger = Logger.getLogger(TorManager.class.getName());
  private volatile boolean torRunning = false;
  private volatile boolean torBootstrapped = false;
  private volatile boolean portsFileWritten = false;
  private boolean externalTor;
  protected String torrc = "config/torrc";
  private boolean torNetworkEnabled = true;

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
    }
  }

  /**
   * Constructs a new TorManager object.
   *
   * @param workingDirectory The directory to run Tor in.
   * @param externalTor True if the Tor process runs outside PTP and shouldn't be stopped.
   */
  public TorManager(String workingDirectory, boolean externalTor) {
    this.workingDirectory = workingDirectory;
    this.externalTor = externalTor;
  }

  /**
   * Checks if a Tor process is already running and otherwise starts a new Tor process.
   */
  public void startTor() throws IOException {
    createWorkingDirectory();
    logger.log(Level.INFO, "TorManager working directory exists.");

    if (checkTorRunning()) {
      torRunning = true;
      torBootstrapped = true;
    } else {
      // Run the Tor process.
      torRunning = runTor();
      
      if (torRunning) {
        readPorts();
        setUpControlConnection();
        torBootstrapped = true;
      }
    }
  }
  
  protected void createWorkingDirectory() throws IOException {
    // Check if the home directory exists, if not try to create it.
    File directory = new File(workingDirectory);
    if (!directory.exists() && !directory.mkdirs()) {
      // Home directory does not exist and was not created.
      throw new IOException("Unable to create missing PTP working directory.");
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
   * Stops the Tor process.
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

    if (!externalTor) {
      shutdownTor();
    }

    if (controlSocket != null) {
      try {
        controlSocket.close();
      } catch (IOException e) {
        logger.log(Level.WARNING, "Failed to close control connection socket.");
      }
    }

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
   * Closes all existing circuits with the supplied destination.
   */
  public void closeCircuits(Identifier destination) {
    if (controlConn == null) {
      logger.log(Level.WARNING, "No control connection open.");
      return;
    }

    try {
      List<String> commands = new LinkedList<String>();
      commands.add("stream-status");
      Map<String, String> streamStatus = controlConn.getInfo(commands);
      
      Collection<String> lines = streamStatus.values();
      
      List<String> circIds = new LinkedList<String>();
      
      for (String line : lines) {
        if (line.equals("")) {
          continue;
        }
        // StreamID SP StreamStatus SP CircuitID SP Target CRLF
        String[] fields = line.split(" ");
        
        if (fields.length != 4) {
          logger.log(Level.WARNING, "Invalid response to GETINFO stream-status");
          continue;
        }
        
        if (fields[3] != null && fields[3].startsWith(destination.getTorAddress()
            ) && fields[2] != null) {
          circIds.add(fields[2]);
        }
      }
      
      for (String circId : circIds) {
        logger.log(Level.INFO, "Closing circuit " + circId);
        controlConn.closeCircuit(circId, false);
      }
      
    } catch (IOException e) {
      logger.log(Level.WARNING, "Failed to close circuits");
    }
  }

  public void changeNetwork(boolean enable) {
    if (controlConn == null) {
      logger.log(Level.WARNING, "No control connection open.");
      return;
    }

    if (torNetworkEnabled == enable) {
      return;
    }

    try {
      logger.log(Level.INFO, (enable ? "En" : "Dis") + "abling network for Tor");

      controlConn.setConf("DisableNetwork", enable ? "0" : "1");
      torNetworkEnabled = enable;
    } catch (IOException e) {
      logger.log(Level.WARNING, "Failed to " + (enable ? "en" : "dis") + "able network");
    }
  }


  /**
   * Stops the Tor process.
   */
  protected void shutdownTor() {
    if (!torRunning) {
      return;
    }

    if (controlConn == null) {
      logger.log(Level.WARNING, "No control connection open. Can't shutdown Tor.");
      return;
    }

    try {
      logger.log(Level.INFO, "TorManager stopping Tor process.");
      if (process != null) {
        logger.log(Level.INFO, "Killing own Tor process");
        process.destroy();
      } else {
        controlConn.shutdownTor(Constants.shutdownsignal);

        logger.log(Level.INFO, "TorManager sent shutdown signal.");
      }

      // Delete the TorManager ports file.
      if (portsFile.delete()) {
        logger.log(Level.INFO, "Deleted TorManager ports file.");
      } else {
        logger.log(Level.WARNING, "Failed to delete ports file " + portsFile.getAbsolutePath());
      }
    } catch (IOException e) {
      logger.log(Level.WARNING,
          "TorManager caught an IOException while closing Tor process: " + e.getMessage());
    }
  }

  private boolean checkTorRunning() throws IOException {
    boolean running = false;
    // Get a handle on the TorManager ports file.
    portsFile = new File(workingDirectory + File.separator + Constants.tormanagerportsfile);

    logger.log(Level.INFO, "Read Tor working directory: " + workingDirectory.toString());
    logger.log(Level.INFO, "TorManager ports file is: " + portsFile.getAbsolutePath());

    if (portsFile.exists()) {
      logger.log(Level.INFO, "TorManager checking if Tor is running.");

      readPorts();
      // Attempt a JTorCtl connection to the Tor process. If Tor is not running the connection
      // will not succeed.
      try {
        logger.log(Level.INFO, "TorManager attempting to connect to the Tor process.");

        setUpControlConnection();

        running = true;

      } catch (IOException e) {
        logger.log(Level.INFO, "TorManager could not connect to the Tor process");
      }
    }
    logger.log(Level.INFO,
        "TorManager checked if Tor is running: " + (torRunning ? "running" : "not running"));
    return running;
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
    torrcFile += torrc;

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
      
      // Wait until ports file is written
      ports.join();
    } catch (IOException e) {
      logger.log(Level.WARNING,
          "TorManager thread caught an IOException when starting Tor: " + e.getMessage());
      return false;
    } catch (InterruptedException e) {
      logger.log(Level.WARNING, "Failed to join PortsThread");
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

  private void setUpControlConnection() throws IOException {
    controlSocket = new Socket(Constants.localhost, torControlPort);
    controlConn = new TorControlConnection(controlSocket);
    controlConn.authenticate(new byte[0]);
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
