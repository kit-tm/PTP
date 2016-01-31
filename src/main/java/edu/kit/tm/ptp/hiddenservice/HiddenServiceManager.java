package edu.kit.tm.ptp.hiddenservice;

import edu.kit.tm.ptp.Configuration;
import edu.kit.tm.ptp.Identifier;
import edu.kit.tm.ptp.utility.Constants;

import net.freehaven.tor.control.TorControlConnection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the hidden service directory.
 *
 * @author Timon Hackenjos
 */
public class HiddenServiceManager {
  /** The logger for this class. */
  private Logger logger = Logger.getLogger(HiddenServiceManager.class.getName());

  /** The parameters of this client. */
  private final Configuration configuration;

  /** The current hidden service sub-directory. */
  private String directory = null;

  /** The currently used hidden service identifier. */
  private Identifier currentIdentifier = null;

  /** The port the current hidden service is listening on. */
  private int port;

  /** The raw API lock file. */
  private LockFile apiLock = null;

  /** Hidden service lock file. */
  private File hiddenServiceLock = null;

  /**
   * Constructs a new HiddenServiceManager.
   * 
   * @param directory The directory to use for the hidden service or null.
   * @param port The port the hidden service is listening on.
   */
  public HiddenServiceManager(Configuration configuration, String directory, int port)
      throws IOException {

    this.configuration = configuration;

    // Set the hidden service directory.
    this.directory = directory == null ? Constants.hiddenserviceprefix + port
        : Constants.hiddenserviceprefix + directory;

    this.port = port;

    // Check if the hidden service directory exists, if not create it.
    File hiddenServiceDirectory = new File(configuration.getHiddenServiceDirectory());
    if (!hiddenServiceDirectory.exists() && !hiddenServiceDirectory.mkdirs()) {
      throw new IOException("Could not create hidden service directory!");
    }

    // Check if the lock file exists, if not create it.
    File lockFile =
        new File(configuration.getWorkingDirectory() + File.separator + Constants.rawapilockfile);
    if (!lockFile.exists() && !lockFile.createNewFile()) {
      throw new IOException("Could not create raw API lock file!");
    }
    apiLock = new LockFile(lockFile);
  }

  public void createHiddenService() throws IOException {
    setUpHiddenService(false);
  }

  public void reuseHiddenService() throws IOException {
    setUpHiddenService(true);
  }

  /**
   * Deletes the currently used hidden service directory.
   * 
   * @throws IOException If an error occurs while deleting the directory.
   */
  public void deleteHiddenService() throws IOException {
    try {
      apiLock.lock();
      deleteHiddenServiceDirectory(directory);

      directory = null;
      currentIdentifier = null;
    } finally {
      apiLock.release();
    }
  }

  public Identifier getHiddenServiceIdentifier() {
    return currentIdentifier;
  }

  /**
   * Removes the lock from the hidden service directory.
   */
  public void close() {
    if (hiddenServiceLock != null && hiddenServiceLock.exists()) {
      hiddenServiceLock.delete();
    }
  }

  private void setUpHiddenService(boolean reuse) throws IOException {
    try {
      // Block until a lock on the raw API lock file is available.
      logger.log(Level.INFO, "Client acquiring lock on raw API lock file.");
      apiLock.lock();
      
      if (reuse && currentIdentifier != null) {
        // Hidden service is already setup
        return;
      }
      
      if (hiddenServiceLock != null && hiddenServiceLock.exists()) {
        // New identifier is requested. Delete lock file of last hidden service directory
        hiddenServiceLock.delete();
      }

      // Search for hidden services to reuse
      // or delete them if a new identifier is requested
      String freeHsDir = checkHiddenServices(reuse);

      if (freeHsDir != null) {
        logger.log(Level.INFO, "Reusing hidden service directory " + freeHsDir);
        directory = freeHsDir;
      } else {
        logger.log(Level.INFO, "Creating new hidden service directory " + directory);
        newHiddenService();
      }

      writePortFile();

      registerHiddenServices();

      currentIdentifier = new Identifier(readIdentifier(directory));

    } finally {
      // Release the lock, if acquired.
      logger.log(Level.INFO, "Client releasing the lock on the raw API lock file.");
      apiLock.release();

    }
  }


  private String checkHiddenServices(boolean reuse) throws IOException {
    File hiddenServiceDirectory = new File(configuration.getHiddenServiceDirectory());
    String freeHsDir = null;

    // Search for a valid hidden service directory
    for (File hiddenService : hiddenServiceDirectory.listFiles()) {
      // Skip over any files in the directory.
      if (!hiddenService.isDirectory()) {
        continue;
      }

      // Skip over any directories without the hidden service prefix.
      String name = hiddenService.getName();
      if (!name.startsWith(Constants.hiddenserviceprefix)) {
        continue;
      }

      // Get the hidden service lock file
      File hsLockFile = new File(hiddenService + File.separator + Constants.hiddenservicelockfile);
      if (hsLockFile.exists()) {
        continue;
      }

      if (!reuse) {
        logger.log(Level.INFO, "Deleting hidden service directory " + name);
        deleteHiddenServiceDirectory(name);
      }

      if (reuse && freeHsDir == null) {
        if (!hsLockFile.createNewFile()) {
          continue;
        }

        // We reuse the dir. Save the lock and release it later
        freeHsDir = name;
        hiddenServiceLock = hsLockFile;

        logger.log(Level.INFO, "Found hidden service directory to reuse " + freeHsDir);
      }
    }

    return freeHsDir;
  }

  private void newHiddenService() throws IOException {
    File hsDir = new File(configuration.getHiddenServiceDirectory() + File.separator + directory);
    File hsLockFile = new File(hsDir + File.separator + Constants.hiddenservicelockfile);

    if (!hsDir.exists() && !hsDir.mkdir()) {
      throw new IOException("Unable to create the hidden service directory!");
    }

    if (!hsLockFile.exists() && !hsLockFile.createNewFile()) {
      throw new IOException("Unable to create the hidden service lock file!");
    }

    hiddenServiceLock = hsLockFile;
  }

  private void writePortFile() throws IOException {
    File portFile = new File(configuration.getHiddenServiceDirectory() + File.separator + directory
        + File.separator + Constants.portfile);
    ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(portFile, false));
    stream.writeInt(port);
    stream.close();
  }


  /**
   * Creates a Tor hidden service by connecting to the Tor control port and invoking JTorCtl. The
   * directory of the hidden service must already be present.
   *
   * @throws IOException Throws an IOException when the Tor control socket is not reachable, or if
   *         the Tor authentication fails.
   */
  private void registerHiddenServices() throws IOException {
    logger.log(Level.INFO, "Registering hidden services.");
    logger.log(Level.INFO, "Opening socket on " + Constants.localhost + ":"
        + configuration.getTorControlPort() + " to control Tor.");
    // Connect to the Tor control port.
    Socket socket = new Socket(Constants.localhost, configuration.getTorControlPort());
    logger.log(Level.INFO, "Fetching JTorCtl connection.");
    TorControlConnection conn = new TorControlConnection(socket);
    logger.log(Level.INFO, "Authenticating the connection.");
    // Authenticate the connection.
    conn.authenticate(configuration.getAuthenticationBytes());

    // Set the properties for the hidden service configuration.
    LinkedList<String> properties = new LinkedList<String>();
    File hiddenServiceDirectory = new File(configuration.getHiddenServiceDirectory());

    // Read the hidden service directories in the hidden service root directory, and add them to the
    // new hidden service configuration.
    for (File hiddenService : hiddenServiceDirectory.listFiles()) {
      // Skip over any files in the directory.
      if (!hiddenService.isDirectory()) {
        continue;
      }

      // Fix directory permissions so that Tor doesn't complain
      hiddenService.setReadable(false, false);
      hiddenService.setReadable(true, true);
      hiddenService.setWritable(false, false);
      hiddenService.setWritable(true, true);
      hiddenService.setExecutable(false, false);
      hiddenService.setExecutable(true, true);

      // Skip over any directories without the hidden service prefix.
      String name = hiddenService.getName();
      if (!name.startsWith(Constants.hiddenserviceprefix)) {
        continue;
      }
      // Get the port file.
      File portFile = new File(hiddenService + File.separator + Constants.portfile);
      if (!portFile.exists()) {
        continue;
      }

      final int port;

      try {
        // Read the port of the hidden service from the port file.
        ObjectInputStream stream = new ObjectInputStream(new FileInputStream(portFile));
        port = stream.readInt();
        stream.close();
      } catch (IOException e) {
        logger.log(Level.WARNING,
            "Received IOException while reading the hidden service port file: " + e.getMessage());
        continue;
      }

      // Add the hidden service property to the configuration properties so far.
      properties.add(Constants.hsdirkeyword + " " + hiddenService.getAbsolutePath());
      properties.add(Constants.hsportkeyword + " " + configuration.getHiddenServicePort() + " "
          + Constants.localhost + ":" + port);
    }

    logger.log(Level.INFO, "Setting configuration:" + Constants.newline + properties.toString());
    conn.setConf(properties);

    logger.log(Level.INFO, "Registered hidden services.");
  }



  /**
   * Deletes the hidden service directory.
   *
   * @throws IOException Propagates any IOException that occured during deletion.
   */
  private void deleteHiddenServiceDirectory(String directory) throws IOException {
    // If the hidden service was not created, there is nothing to delete.
    if (directory == null) {
      return;
    }

    logger.log(Level.INFO, "Deleting hidden service directory.");
    File hostname = new File(configuration.getHiddenServiceDirectory() + File.separator + directory
        + File.separator + Constants.hostname);
    File hiddenservice =
        new File(configuration.getHiddenServiceDirectory() + File.separator + directory);
    File privatekey = new File(configuration.getHiddenServiceDirectory() + File.separator
        + directory + File.separator + Constants.prkey);
    File port = new File(configuration.getHiddenServiceDirectory() + File.separator + directory
        + File.separator + Constants.portfile);
    File lockFile = new File(configuration.getHiddenServiceDirectory() + File.separator + directory
        + File.separator + Constants.hiddenservicelockfile);

    boolean hostnameDeleted = hostname.delete();
    logger.log(Level.INFO, "Deleted hostname file: " + (hostnameDeleted ? "yes" : "no"));
    boolean prkeyDeleted = privatekey.delete();
    logger.log(Level.INFO, "Deleted private key file: " + (prkeyDeleted ? "yes" : "no"));
    boolean portDeleted = port.delete();
    logger.log(Level.INFO, "Deleted port file: " + (portDeleted ? "yes" : "no"));
    boolean lockFileDeleted = lockFile.delete();
    logger.log(Level.INFO, "Deleted hidden service lock file: " + (lockFileDeleted ? "yes" : "no"));
    boolean directoryDeleted = hiddenservice.delete();
    logger.log(Level.INFO,
        "Deleted hidden service directory: " + (directoryDeleted ? "yes" : "no"));

    if (!directoryDeleted) {
      throw new IOException(
          "Client failed to delete hidden service directory: " + hiddenservice.getAbsolutePath());
    }
  }

  private String readIdentifier(String hsDir) throws IOException {
    File hostname = new File(configuration.getHiddenServiceDirectory() + File.separator + hsDir
        + File.separator + Constants.hostname);

    logger.log(Level.INFO, "Reading identifier from file: " + hostname);

    BufferedReader buffer =
        new BufferedReader(new InputStreamReader(new FileInputStream(hostname)));

    logger.log(Level.INFO, "Reading line.");
    String identifier = buffer.readLine();

    logger.log(Level.INFO, "Closing file stream.");
    buffer.close();

    logger.log(Level.INFO, "Read identifier: " + identifier);

    return identifier;
  }
}
