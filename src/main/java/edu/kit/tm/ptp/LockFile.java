package edu.kit.tm.ptp;

import edu.kit.tm.ptp.utility.Constants;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;

public class LockFile {
  private RandomAccessFile raf;
  private FileLock lock;
  private File file;

  public LockFile(File file) {
    this.file = file;
  }

  /**
   * Acquires lock for the file. Blocks until the lock was acquired successfully.
   */
  public void lock() throws IOException {
    raf = new RandomAccessFile(file, Constants.readwriterights);
    lock = raf.getChannel().lock();
  }

  /**
   * Tries to acquire lock for the file. The method doesn't block.
   * 
   * @return Returns true if the lock could be acquired immediately.
   */
  public boolean tryLock() throws IOException {
    raf = new RandomAccessFile(file, Constants.readwriterights);

    lock = raf.getChannel().tryLock();

    return lock != null;
  }

  /**
   * Releases a previously acquired lock.
   * 
   */
  public void release() throws IOException {
    if (lock != null) {
      lock.release();
      raf.close();
      lock = null;
    }
  }
}
