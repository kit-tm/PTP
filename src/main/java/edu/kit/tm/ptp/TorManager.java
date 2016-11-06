package edu.kit.tm.ptp;

import edu.kit.tm.ptp.utility.Constants;

import net.freehaven.tor.control.ConfigEntry;
import net.freehaven.tor.control.TorControlConnection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A manager for the Tor process of the API.
 *
 * @author Timon Hackenjos
 * @author Simeon Andreev
 *
 */
public class TorManager {
  /** The Tor working directory path. */
  protected final String workingDirectory;
  /** The managed Tor process. */
  protected Process process = null;
  /** The control port number of the Tor process. */
  private volatile int torControlPort = -1;
  /** The SOCKS proxy port number of the Tor process. */
  private volatile int torSocksProxyPort = -1;
  private TorControlConnection controlConn = null;
  private Socket controlSocket = null;

  private static final Logger logger = Logger.getLogger(TorManager.class.getName());
  private volatile boolean torRunning = false;
  protected String torrc = Constants.torrcfile;
  private boolean externalTor;
  private boolean torNetworkEnabled = false;
  private final File controlPortFile;
  private final TorEventHandler torEvent = new TorEventHandler(this);
  private List<SOCKSProxyListener> proxyPortListeners = new LinkedList<SOCKSProxyListener>();
  private Configuration config;

  /**
   * Constructs a new TorManager object that uses an already running Tor process in the
   * workingDirectory or starts a new Tor process.
   *
   * @param workingDirectory The directory to run Tor in.
   * @param config The PTP configuration.
   */
  public TorManager(String workingDirectory, Configuration config) {
    this.workingDirectory = workingDirectory;
    this.externalTor = false;

    this.controlPortFile =
        new File(workingDirectory + File.separator + Constants.torControlPortFileName);
    this.config = config;
  }
  
  /**
   * Constructs a new TorManager object using an already running Tor process.
   * 
   * @param controlPort The control port of the Tor process.
   * @param config The PTP configuration.
   */
  public TorManager(int controlPort, Configuration config) {
    this.workingDirectory = null;
    this.controlPortFile = null;
    this.torControlPort = controlPort;
    this.externalTor = true;
    this.config = config;
  }

  /**
   * Interface to listen for SOCKS proxy configuration.
   */
  public interface SOCKSProxyListener {
    void updateSOCKSProxy(String socksHost, int socksProxyPort);
  }

  /**
   * Checks if a Tor process is already running and otherwise starts a new Tor process.
   * 
   * @return True if a Tor process is running now.
   */
  public boolean startTor() {
    try {
      if (!externalTor) {
        createWorkingDirectory();
      }
    } catch (IOException e) {
      logger.log(Level.WARNING, "Failed to create workingDirectory");
    }
    
    logger.log(Level.INFO, "TorManager working directory exists.");

    try {
      if (externalTor) {
        torRunning = checkTorRunning(torControlPort);
        if (!torRunning) {
          return false;
        }
      } else {
        // Check if a Tor process is already running
        torRunning = readControlPortFile(false) && checkTorRunning(torControlPort);

        if (!torRunning) {
          if (controlPortFile.exists() && !controlPortFile.delete()) {
            logger.log(Level.WARNING, "Failed to delete " + controlPortFile.getAbsolutePath());
          }
          // Run the Tor process.
          torRunning = runTor();
          
          if (!torRunning || !readControlPortFile(true)) {
            return false;
          }

          setUpControlConnection(torControlPort);
        }
      }
      
      torNetworkEnabled = !getDisableNetwork();
      getAndUpdateSOCKSProxy();
      setUpEventHandler();

    } catch (IOException e) {
      logger.log(Level.WARNING, "Failed to start/connect to Tor process " + e.getMessage());
      torRunning = false;
      return false;
    }

    return torRunning;
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
   * Stops the Tor process.
   */
  public void stopTor() {
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
    controlConn = null;
    controlSocket = null;
  }
  
  /**
   * Adds a listener for SOCKS proxy changes.
   */
  public void addSOCKSProxyListener(SOCKSProxyListener listener) {
    proxyPortListeners.add(listener);
  }

  /**
   * Returns the control port number of the Tor process.
   */
  public int getTorControlPort() {
    if (!torRunning) {
      throw new IllegalStateException();
    }

    return torControlPort;
  }

  /**
   * Returns the SOCKS proxy port number of the Tor process.
   */
  public int getTorSOCKSPort() {
    if (!torRunning) {
      throw new IllegalStateException();
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

      Map<String, String> response = controlConn.getInfo(commands);
      String streamStatus = response.get("stream-status");

      List<String> circIds = new LinkedList<String>();

      for (String line : streamStatus.split("\n")) {
        if (line.equals("")) {
          continue;
        }
        // StreamID SP StreamStatus SP CircuitID SP Target CRLF
        String[] fields = line.split(" ");

        if (fields.length != 4) {
          logger.log(Level.WARNING, "Invalid response to GETINFO stream-status");
          continue;
        }

        if (fields[3] != null && fields[3].startsWith(destination.getTorAddress())
            && fields[2] != null) {
          circIds.add(fields[2]);
        }
      }

      for (String circId : circIds) {
        // CircuitID 0 is used when a circuit is being created. It can't be closed then
        if (circId != "0") {
          logger.log(Level.INFO, "Closing circuit " + circId);
          controlConn.closeCircuit(circId, false);
        }
      }

    } catch (IOException e) {
      logger.log(Level.WARNING, "Failed to close circuits");
    }
  }

  /**
   * En/Disables the network for Tor using the DisableNetwork option.
   */
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

      controlConn.setConf(Constants.torDisableNetwork, enable ? "0" : "1");
      torNetworkEnabled = enable;

      // Try to get current SOCKS proxy port
      getAndUpdateSOCKSProxy();
    } catch (IOException e) {
      logger.log(Level.WARNING, "Failed to " + (enable ? "en" : "dis") + "able network");
    }
  }
  
  /**
   * Sets configuration options for Tor.
   * 
   * @param properties The collection of options.
   */
  public void setConf(Collection<String> properties) throws IOException {
    if (controlConn == null) {
      logger.log(Level.WARNING, "No control connection open.");
      return;
    }
    
    controlConn.setConf(properties);
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

      // Delete ControlPortFile
      if (controlPortFile.delete()) {
        logger.log(Level.INFO, "Deleted ControlPortFile.");
      } else {
        logger.log(Level.WARNING,
            "Failed to delete ControlPortFile " + controlPortFile.getAbsolutePath());
      }
    } catch (IOException e) {
      logger.log(Level.WARNING,
          "TorManager caught an IOException while closing Tor process: " + e.getMessage());
    }
  }

  private boolean getDisableNetwork() throws IOException {
    List<ConfigEntry> configEntries = controlConn.getConf(Constants.torDisableNetwork);

    for (ConfigEntry entry : configEntries) {
      if (entry != null && entry.key != null && entry.value != null
          && entry.key.equals(Constants.torDisableNetwork)) {
        return Integer.parseInt(entry.value) == 1;
      }
    }

    // Assume the network is enabled
    return false;
  }
  
  private int getSOCKSProxyPort() {
    if (!torRunning) {
      logger.log(Level.WARNING, "Tor isn't running");
    }
    
    try {
      String result = controlConn.getInfo(Constants.torGetInfoSOCKSProxy);
      
      if (result != null) {
        String[] listeners = result.split(" ");
        
        if (listeners.length <= 0 || listeners[0] == null) {
          return -1;
        }
        
        // Strip quotes
        listeners[0] = listeners[0].replace("\"", "");
        
        // Get port
        String[] listener = listeners[0].split(":");
        
        if (listener.length < 2 || listener[1] == null) {
          return -1;
        }
        
        return Integer.parseInt(listener[1]);
      }
    } catch (IOException e) {
      logger.log(Level.WARNING, "Failed to get Tor SOCKS port");
    }
    
    return -1;
  }

  private boolean checkTorRunning(int controlPort) {
    logger.log(Level.INFO, "TorManager checking if Tor is running.");

    // Attempt a JTorCtl connection to the Tor process. If Tor is not running the connection
    // will not succeed.
    try {
      logger.log(Level.INFO, "TorManager attempting to connect to the Tor process.");

      setUpControlConnection(torControlPort);

      return true;

    } catch (IOException e) {
      logger.log(Level.INFO, "TorManager could not connect to the Tor process");
    }

    return false;
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
    
    //if (!updateTorRc(torrcFile)) {
    //  return false;
    //}

    try {
      /** The parameters for the Tor execution command. */
      final String[] parameters = {
          /** The Tor executable file. */
          torFile,
          /** The Tor configuration file option. */
          Constants.torrcoption,
          torrcFile,
          /** The Tor working directory option. */
          Constants.datadiroption,
          workingDirectory,
          // The option to write the port to a file.
          Constants.ctlportwriteoption,
          controlPortFile.getAbsolutePath()
      };

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

      if (!waitForConfiguration(Constants.torStartTimeout)) {
        logger.log(Level.WARNING, "Waiting for Tor start timed out.");
        return false;
      }
    } catch (IOException e) {
      logger.log(Level.WARNING,
          "TorManager thread caught an IOException when starting Tor: " + e.getMessage());
      return false;
    } catch (InterruptedException e) {
      logger.log(Level.WARNING, "Interrupted while waiting for the Tor ports ");
      return false;
    }

    return true;
  }


  private boolean waitForConfiguration(long timeout) throws InterruptedException {
    long start = System.currentTimeMillis();

    while (!controlPortFile.exists() && System.currentTimeMillis() - start < timeout) {
      Thread.sleep(1 * 1000);
    }

    return controlPortFile.exists();
  }

  private boolean readControlPortFile(boolean failureCritical) {
    Level logLevel = failureCritical ? Level.WARNING : Level.INFO;

    BufferedReader reader = null;
    try {
      reader = new BufferedReader(
          new InputStreamReader(new FileInputStream(controlPortFile), Constants.charset));
      String line = reader.readLine();

      if (line != null) {
        String[] values = line.split(":");
        
        if (values.length >= 2 && values[1] != null) {
          torControlPort = Integer.parseInt(values[1]);
          return true;
        }
      }
    } catch (FileNotFoundException e) {
      logger.log(logLevel,
          "ControlPortFile " + controlPortFile.getAbsolutePath() + " doesn't exist");
    } catch (IOException e) {
      logger.log(logLevel,
          "Failed to read ControlPortFile " + controlPortFile.getAbsolutePath());
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
          logger.log(Level.WARNING, "Failed to close BufferedReader");
        }
      }
    }

    return false;
  }

  private void setUpControlConnection(int controlPort) throws IOException {
    controlSocket = new Socket(Constants.localhost, controlPort);
    controlConn = new TorControlConnection(controlSocket);

    if (config == null) {
     controlConn.authenticate(new byte[0]);
    } else {
      controlConn.authenticate(config.getAuthenticationBytes());
    }
  }
  
  private void setUpEventHandler() throws IOException {
    controlConn.setEventHandler(torEvent);
    controlConn.setEvents(torEvent.getEvents());
  }

  private void getAndUpdateSOCKSProxy() {
    torSocksProxyPort = getSOCKSProxyPort();

    if (torSocksProxyPort != -1) {
      logger.log(Level.INFO, "Got Tor SOCKS proxy port: " + torSocksProxyPort);
      updateSOCKSProxy(Constants.localhost, torSocksProxyPort);
    }
  }
  
  private void updateSOCKSProxy(String socksHost, int socksProxyPort) {
    for (SOCKSProxyListener listener : proxyPortListeners) {
      listener.updateSOCKSProxy(socksHost, socksProxyPort);
    }
  }
}
