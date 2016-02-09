package edu.kit.tm.ptp.hiddenservice;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Keeps a list of all lock files and ensures that only one instance is used per file. 
 * 
 * @author Timon Hackenjos
 *
 */
public class LockFileFactory {
  private static Map<File, LockFile> locks = new HashMap<File, LockFile>();
  
  /**
   * Return a LockFile object for the specified file.
   * 
   * @throws IOException If getting the full file path fails.
   */
  public static LockFile getLockFile(File file) throws IOException {
    LockFile lockFile = locks.get(file.getCanonicalFile());
    
    if (lockFile != null) {
      return lockFile;
    }
    
    lockFile = new LockFile(file.getCanonicalFile());
    locks.put(file.getCanonicalFile(), lockFile);
    return lockFile;
  }
}
