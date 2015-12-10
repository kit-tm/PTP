package edu.kit.tm.ptp;

import java.util.concurrent.atomic.AtomicInteger;

public class TestHelper {
  public static void wait(AtomicInteger condition, int expected, long millis) {
    long start = System.currentTimeMillis();

    while (condition.get() != expected && (System.currentTimeMillis() - start < millis)) {
      try {
        Thread.sleep(1 * 1000);
      } catch (InterruptedException e) {
        // Sleeping was interrupted. Do nothing.
      }
    }
  }
}
