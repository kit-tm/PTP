package edu.kit.tm.ptp;

import edu.kit.tm.ptp.raw.Configuration;
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
import java.io.RandomAccessFile;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class.
 *
 * @author Timon Hackenjos
 */
public class HiddenServiceConfiguration {
  /**
   * The logger for this class.
   */
  private Logger logger = Logger.getLogger(Constants.clientlogger);
  /**
   * The parameters of this client.
   */
  private final Configuration configuration;
  /**
   * The raw API lock file.
   */
  private final File lockFile;
  /**
   * The current hidden service identifier.
   */
  private String identifier = null;
  /**
   * The hidden service sub-directory of this client.
   */
  private String directory = null;
  /**
   * The port the hidden service is listening on.
   */
  private int port;

  public HiddenServiceConfiguration(Configuration configuration, String directory, int port)
      throws IOException {

    this.configuration = configuration;

    // Set the hidden service directory.
    this.directory = directory == null ? Constants.hiddenserviceprefix + port
        : Constants.hiddenserviceprefix + directory;

    // If no hidden service directory is specified, use the / make a hidden service directory for
    // the port we are listening on.
    if (directory == null) {
      this.directory = Constants.hiddenserviceprefix + port;
    }

    // Check if the hidden service directory exists, if not create it.
    File hiddenServiceDirectory = new File(configuration.getHiddenServiceDirectory());
    if (!hiddenServiceDirectory.exists() && !hiddenServiceDirectory.mkdirs()) {
      throw new IOException("Could not create hidden service directory!");
    }

    // Check if the lock file exists, if not create it.
    lockFile =
        new File(configuration.getWorkingDirectory() + File.separator + Constants.rawapilockfile);
    if (!lockFile.exists() && !lockFile.createNewFile()) {
      throw new IOException("Could not create raw API lock file!");
    }

    // Set a default identifier.
    identifier = configuration.getDefaultIdentifier();

    logger.log(Level.INFO, "Client object created (port: " + port + ").");
  }

  /**
   * Returns the local Tor hidden service identifier. Will create a hidden service if none is
   * present by using JTorCtl.
   *
   * @param fresh If true, will always generate a new identifier, even if one is already present.
   * @return The hidden service identifier.
   * @throws IOException Throws an exception when unable to read the Tor hostname file, or when
   *         unable to connect to Tors control socket.
   */
  public String identifier(boolean fresh) throws IOException {
    logger.log(Level.INFO,
        (fresh ? "Fetching a fresh" : "Attempting to reuse (if present) the") + " identifier.");

    RandomAccessFile raf = null;
    FileChannel channel = null;
    FileLock lock = null;
    ObjectOutputStream stream = null;

    try {
      // Block until a lock on the raw API lock file is available.
      logger.log(Level.INFO, "Client acquiring lock on raw API lock file.");
      raf = new RandomAccessFile(lockFile, Constants.readwriterights);
      channel = raf.getChannel();
      lock = channel.lock();

      logger.log(Level.INFO, "Client acquired the lock on raw API lock file.");

      File dir = new File(configuration.getHiddenServiceDirectory() + File.separator + directory);
      File portfile = new File(dir + File.separator + Constants.portfile);
      File hostname = new File(dir + File.separator + Constants.hostname);
      boolean create = !dir.exists() || !hostname.exists()
          || !new File(dir + File.separator + Constants.prkey).exists() || !portfile.exists();

      // If a fresh identifier is requested, delete the current hidden service directory.
      if (fresh && !create) {
        deleteHiddenService();
      }

      // Create the hidden service directory if necessary.
      if (!dir.exists() && !dir.mkdir()) {
        throw new IOException("Unable to create the hidden service directory!");
      }

      // Create a hidden service with JTorCtl.
      // Write the port of the receiver to the port file of the hidden service directory.
      logger.log(Level.INFO, "Writing to port file.");
      // final int port = receiver.getPort();
      stream = new ObjectOutputStream(new FileOutputStream(portfile, false));
      stream.writeInt(port);
      stream.close();
      stream = null;

      logger.log(Level.INFO, "Creating hidden service.");
      createHiddenService();

      // Read the content of the Tor hidden service hostname file.
      identifier = readIdentifier(hostname);
      logger.log(Level.INFO, "Fetched hidden service identifier: " + identifier);

      return identifier;
    } finally {
      // Release the lock, if acquired.
      logger.log(Level.INFO, "Client releasing the lock on the raw API lock file.");

      if (lock != null) {
        lock.release();
        // Close the lock file.
        channel.close();
        raf.close();
      }

      if (stream != null) {
        stream.close();
      }
    }
  }

  /**
   * Creates a Tor hidden service by connecting to the Tor control port and invoking JTorCtl. The
   * directory of the hidden service must already be present.
   *
   * @throws IOException Throws an IOException when the Tor control socket is not reachable, or if
   *         the Tor authentication fails.
   */
  private void createHiddenService() throws IOException {
    logger.log(Level.INFO, "Creating hidden service.");
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
        ;
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

    logger.log(Level.INFO, "Created hidden service.");
  }

  /**
   * Deletes the hidden service directory.
   *
   * @throws IOException Propagates any IOException that occured during deletion.
   */
  public void deleteHiddenService() throws IOException {
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

    boolean hostnameDeleted = hostname.delete();
    logger.log(Level.INFO, "Deleted hostname file: " + (hostnameDeleted ? "yes" : "no"));
    boolean prkeyDeleted = privatekey.delete();
    logger.log(Level.INFO, "Deleted private key file: " + (prkeyDeleted ? "yes" : "no"));
    boolean portDeleted = port.delete();
    logger.log(Level.INFO, "Deleted port file: " + (portDeleted ? "yes" : "no"));
    boolean directoryDeleted = hiddenservice.delete();
    logger.log(Level.INFO,
        "Deleted hidden service directory: " + (directoryDeleted ? "yes" : "no"));

    if (!directoryDeleted || !hostnameDeleted || !prkeyDeleted || !portDeleted) {
      throw new IOException(
          "Client failed to delete hidden service directory: " + hiddenservice.getAbsolutePath());
    }
  }

  /**
   * Reads the Tor hidden service identifier from the hostname file.
   *
   * @param hostname The file from which the Tor hidden service identifier should be read.
   * @return The string identifier representing the Tor hidden service identifier.
   * @throws IOException Throws an IOException when unable to read the Tor hidden service hostname
   *         file.
   */
  private String readIdentifier(File hostname) throws IOException {
    logger.log(Level.INFO, "Reading identifier from file: " + hostname);
    FileInputStream stream = new FileInputStream(hostname);
    InputStreamReader reader = new InputStreamReader(stream);
    BufferedReader buffer = new BufferedReader(reader);

    logger.log(Level.INFO, "Reading line.");
    String identifier = buffer.readLine();

    logger.log(Level.INFO, "Closing file stream.");
    buffer.close();

    logger.log(Level.INFO, "Read identifier: " + identifier);
    return identifier;
  }
}
