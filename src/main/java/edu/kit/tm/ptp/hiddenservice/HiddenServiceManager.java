package edu.kit.tm.ptp.hiddenservice;

import edu.kit.tm.ptp.Configuration;
import edu.kit.tm.ptp.Identifier;
import edu.kit.tm.ptp.TorManager;
import edu.kit.tm.ptp.utility.Constants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
  private final Logger logger = Logger.getLogger(HiddenServiceManager.class.getName());

  /** The parameters of this client. */
  private final Configuration configuration;

  /** The port the current hidden service is listening on. */
  private final int port;

  private final TorManager torManager;
  /** The raw API lock file. */
  private final LockFile apiLock;

  /** The current hidden service sub-directory. */
  private volatile String currentDirectory = null;

  /** The name of the directory to use as hiddenService. */
  private volatile String hiddenServiceDirectoryName;

  /** The currently used hidden service identifier. */
  private volatile Identifier currentIdentifier = null;

  /** Hidden service lock file. */
  private volatile File hiddenServiceLock = null;


  /**
   * Constructs a new HiddenServiceManager.
   * 
   * @param hiddenServiceDirectoryName The name of the directory to use for the hidden service or
   *        null. The directory must not be in use by any other instance.
   * @param port The port the hidden service is listening on.
   */
  public HiddenServiceManager(Configuration configuration, String hiddenServiceDirectoryName,
      int port, TorManager torManager) throws IOException {

    this.configuration = configuration;
    this.hiddenServiceDirectoryName = hiddenServiceDirectoryName;
    this.port = port;
    this.torManager = torManager;

    // Check if the hidden service directory exists, if not create it.
    File hiddenServicesDirectory = new File(configuration.getHiddenServicesDirectory());
    if (!hiddenServicesDirectory.exists() && !hiddenServicesDirectory.mkdirs()) {
      throw new IOException("Could not create hidden service directory!");
    }

    // Check if the lock file exists, if not create it.
    File lockFile =
        new File(configuration.getWorkingDirectory() + File.separator + Constants.rawapilockfile);
    if (!lockFile.exists() && !lockFile.createNewFile()) {
      throw new IOException("Could not create raw API lock file!");
    }
    apiLock = LockFileFactory.getLockFile(lockFile);
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
      deleteHiddenServiceDirectory(currentDirectory);

      currentDirectory = null;
      currentIdentifier = null;
    } finally {
      apiLock.release();
    }
  }

  public Identifier getHiddenServiceIdentifier() {
    return currentIdentifier;
  }

  public String getHiddenServiceDirectory() {
    return currentDirectory;
  }

  /**
   * Returns the private key file of the currently used hidden service.
   */
  public File getPrivateKeyFile() {
    if (currentDirectory == null) {
      return null;
    }

    return new File(currentDirectory + File.separator + Constants.prkey);
  }

  /**
   * Removes the lock from the hidden service directory.
   */
  public void close() {
    if (hiddenServiceLock != null && hiddenServiceLock.exists()) {
      if (!hiddenServiceLock.delete()) {
        logger.log(Level.WARNING,
            "Failed to delete lock file " + hiddenServiceLock.getAbsolutePath());
      }
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
        if (!hiddenServiceLock.delete()) {
          logger.log(Level.WARNING,
              "Failed to delete lock file " + hiddenServiceLock.getAbsolutePath());
        }
      }

      if (hiddenServiceDirectoryName != null) {
        currentDirectory = configuration.getHiddenServicesDirectory() + File.separator
            + hiddenServiceDirectoryName;
        newHiddenService();
      } else {
        // Search for hidden services to reuse
        // or delete them if a new identifier is requested
        String freeHsDir = checkHiddenServices(reuse);

        if (freeHsDir != null) {
          logger.log(Level.INFO, "Reusing hidden service directory " + freeHsDir);
          currentDirectory = freeHsDir;
        } else {
          logger.log(Level.INFO,
              "Creating new hidden service directory " + currentDirectory);
          currentDirectory = configuration.getHiddenServicesDirectory() + File.separator
              + Constants.hiddenserviceprefix + port;
          newHiddenService();
        }
      }

      writePortFile();

      registerHiddenServices();

      if (!waitForHiddenService(Constants.torCreateHiddenServiceTimeout)) {
        throw new IOException("Waiting for hidden service creation timed out.");
      }

      currentIdentifier = new Identifier(readIdentifier(currentDirectory));

    } finally {
      // Release the lock, if acquired.
      logger.log(Level.INFO, "Client releasing the lock on the raw API lock file.");
      apiLock.release();
    }
  }

  private boolean waitForHiddenService(long timeout) {
    long start = System.currentTimeMillis();

    File key = getPrivateKeyFile();
    File host = getHostFile(currentDirectory);

    while (!(key.exists() && host.exists()) && System.currentTimeMillis() - start < timeout) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        logger.log(Level.WARNING, "Interrupted while waiting for hidden service.");
        return false;
      }
    }

    return key.exists() && host.exists();
  }


  private String checkHiddenServices(boolean reuse) throws IOException {
    File hiddenServicesDirectory = new File(configuration.getHiddenServicesDirectory());
    String freeHsDir = null;

    File[] dirs = hiddenServicesDirectory.listFiles();

    if (dirs == null) {
      return freeHsDir;
    }

    // Search for a valid hidden service directory
    for (File hiddenService : dirs) {
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
        deleteHiddenServiceDirectory(hiddenService.getAbsolutePath());
      }

      if (reuse && freeHsDir == null) {
        if (!hsLockFile.createNewFile()) {
          continue;
        }

        // We reuse the dir. Save the lock and release it later
        freeHsDir = hiddenService.getAbsolutePath();
        hiddenServiceLock = hsLockFile;

        logger.log(Level.INFO, "Found hidden service directory to reuse " + freeHsDir);
      }
    }

    return freeHsDir;
  }

  private void newHiddenService() throws IOException {
    File hsDir = new File(currentDirectory);
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
    File portFile = new File(currentDirectory + File.separator + Constants.portfile);
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

    // Set the properties for the hidden service configuration.
    LinkedList<String> properties = new LinkedList<String>();
    File hiddenServicesDirectory = new File(configuration.getHiddenServicesDirectory());

    File[] dirs = hiddenServicesDirectory.listFiles();
    // Read the hidden service directories in the hidden service root directory, and add them
    // to the
    // new hidden service configuration.
    if (dirs != null) {
      for (File hiddenService : dirs) {
        // Skip over any files in the directory.
        if (!hiddenService.isDirectory()) {
          continue;
        }

        // Fix directory permissions so that Tor doesn't complain
        if (!(hiddenService.setReadable(false, false) && hiddenService.setReadable(true, true)
            && hiddenService.setWritable(false, false) && hiddenService.setWritable(true, true)
            && hiddenService.setExecutable(false, false)
            && hiddenService.setExecutable(true, true))) {
          logger.log(Level.WARNING,
              "Failed to set permissions for folder " + hiddenService.getAbsolutePath());
        }

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
        properties.add(Constants.hsversionkeyword + " " + Constants.hsversion);
      }
    }

    logger.log(Level.INFO, "Setting configuration:" + Constants.newline + properties.toString());
    torManager.setConf(properties);

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
    File hostname = new File(directory + File.separator + Constants.hostname);
    File hiddenservice = new File(directory);
    File privatekey = new File(directory + File.separator + Constants.prkey);
    File port = new File(directory + File.separator + Constants.portfile);
    File lockFile = new File(directory + File.separator + Constants.hiddenservicelockfile);

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
      throw new IOException("Client failed to delete hidden service directory: "
          + hiddenservice.getAbsolutePath());
    }
  }

  private String readIdentifier(String hsDir) throws IOException {
    File hostname = getHostFile(hsDir);

    logger.log(Level.INFO, "Reading identifier from file: " + hostname);

    BufferedReader buffer =
        new BufferedReader(new InputStreamReader(new FileInputStream(hostname), Constants.charset));

    logger.log(Level.INFO, "Reading line.");
    String identifier = buffer.readLine();

    logger.log(Level.INFO, "Closing file stream.");
    buffer.close();

    logger.log(Level.INFO, "Read identifier: " + identifier);

    return identifier;
  }

  private File getHostFile(String hsDir) {
    return new File(hsDir + File.separator + Constants.hostname);
  }
}
