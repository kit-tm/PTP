package edu.kit.tm.ptp;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class TestHelper {
  public static void wait(AtomicInteger condition, int expected, long millis) {
    if (condition == null || millis < 0) {
      throw new IllegalArgumentException();
    }
    
    long start = System.currentTimeMillis();

    while (condition.get() != expected && (System.currentTimeMillis() - start < millis)) {
      try {
        Thread.sleep(1 * 1000);
      } catch (InterruptedException e) {
        // Sleeping was interrupted. Do nothing.
      }
    }
  }
  
  public static void wait(AtomicBoolean condition, long millis) {
    if (condition == null || millis < 0) {
      throw new IllegalArgumentException();
    }
    
    long start = System.currentTimeMillis();

    while (!condition.get() && (System.currentTimeMillis() - start < millis)) {
      try {
        Thread.sleep(1 * 1000);
      } catch (InterruptedException e) {
        // Sleeping was interrupted. Do nothing.
      }
    }
  }

  public static void sleep(long millis) {
    if (millis < 0) {
      throw new IllegalArgumentException();
    }
    
    long start = System.currentTimeMillis();
    long elapsed = 0;

    do {
      try {
        Thread.sleep(millis - elapsed);
      } catch (InterruptedException e) {
        // Sleeping was interrupted. Do nothing.
      }
      elapsed = System.currentTimeMillis() - start;
    } while (elapsed < millis);
  }
}
