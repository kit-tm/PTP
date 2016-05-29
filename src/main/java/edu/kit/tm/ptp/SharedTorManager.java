package edu.kit.tm.ptp;

import edu.kit.tm.ptp.hiddenservice.LockFile;
import edu.kit.tm.ptp.hiddenservice.LockFileFactory;
import edu.kit.tm.ptp.utility.Constants;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A manager for the Tor process of the API which allows to share a Tor process between several
 * instances of PTP. Used for testing.
 * 
 * @author Timon Hackenjos
 */

public class SharedTorManager extends TorManager {
  private static final Logger logger = Logger.getLogger(SharedTorManager.class.getName());
  private File lockFile;

  public SharedTorManager(String workingDirectory, boolean externalTor) {
    super(workingDirectory, externalTor);
    torrc = "config/testtorrc";
  }

  @Override
  public void startTor() throws IOException {
    createWorkingDirectory();
    // Check if the lock file exists, if not create it.
    lockFile = new File(workingDirectory + File.separator + Constants.tormanagerlockfile);
    if (!lockFile.exists() && !lockFile.createNewFile()) {
      // Lock file does not exist and was not created.
      throw new IOException("Unable to create missing PTP TorManager lock file.");
    }

    logger.log(Level.INFO, "TorManager lock file is: " + lockFile.getAbsolutePath());

    // Check if another TorManager is currently running a Tor process.
    LockFile lock = null;
    try {
      // Block until a lock on the TorManager lock file is available.
      logger.log(Level.INFO, "TorManager acquiring lock on TorManager lock file.");

      lock = LockFileFactory.getLockFile(lockFile);
      lock.lock();
      RandomAccessFile raf = lock.getRandomAccessFile();

      logger.log(Level.INFO, "TorManager has the lock on the TorManager lock file.");

      super.startTor();

      int numApis;

      if (process != null && process.isAlive()) {
        numApis = 0;
      } else {
        numApis = raf.length() == 0 ? 0 : raf.readInt();
      }

      numApis++;

      raf.seek(0);
      raf.writeInt(numApis);

      logger.log(Level.INFO, "TorManager set lock file counter to: " + (numApis));
    } finally {
      if (lock != null) {
        logger.log(Level.INFO, "TorManager releasing the lock on the TorManager lock file.");
        lock.release();
      }
    }
  }

  @Override
  protected void shutdownTor() {
    if (!torRunning()) {
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
        if (numberOfApis == 1) {
          logger.log(Level.INFO, "TorManager stopping Tor process.");
          super.shutdownTor();
        }
      } else {
        logger.log(Level.WARNING, "TorManager file lock is broken!");
      }

    } catch (IOException e) {
      logger.log(Level.WARNING,
          "TorManager caught an IOException while closing Tor process: " + e.getMessage());
    } finally {
      if (lock != null) {
        logger.log(Level.INFO, "TorManager releasing the lock on the TorManager lock file.");
        lock.release();
      }
    }
  }

}
