package edu.kit.tm.ptp.hiddenservice;

import edu.kit.tm.ptp.utility.Constants;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Use files to synchronize between several java processes and
 * use threads to synchronize between several threads of the
 * same java process.
 * 
 * @author Timon Hackenjos
 */

public class LockFile {
  private RandomAccessFile raf = null;
  private FileLock lock;
  private File file;
  private ReentrantLock threadLock = new ReentrantLock();
  private static final Logger logger = Logger.getLogger(LockFile.class.getName());

  public LockFile(File file) {
    this.file = file;
  }

  /**
   * Acquires lock for the file. Blocks until the lock was acquired successfully.
   */
  public void lock() throws IOException {
    threadLock.lock();
    try {
      raf = new RandomAccessFile(file, Constants.readwriterights);
      lock = raf.getChannel().lock();
    } catch (IOException e) {
      threadLock.unlock();
      throw e;
    }
  }

  /**
   * Releases a previously acquired lock.
   */
  public void release() {
    if (lock != null && !threadLock.isHeldByCurrentThread()) {
      throw new IllegalStateException();
    }
    
    if (lock != null) {
      try {
        lock.release();
      } catch (IOException e) {
        logger.log(Level.WARNING, "Failed to release lock: " + e.getMessage());
      }
      
      try {
        raf.close();
      } catch (IOException e) {
        logger.log(Level.WARNING, "Failed to close file: " + e.getMessage());
      }
      
      lock = null;
    }
    
    if (threadLock.isHeldByCurrentThread()) {
      threadLock.unlock();
    }
  }
  
  /**
   * Returns the RandomAccessFile object of the LockFile.
   */
  public RandomAccessFile getRandomAccessFile() {
    if (raf == null) {
      throw new IllegalStateException();
    }
    
    return raf;
  }
}
