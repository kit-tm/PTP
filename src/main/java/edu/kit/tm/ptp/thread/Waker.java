package edu.kit.tm.ptp.thread;

import java.util.concurrent.Semaphore;

/**
 * Thread to wake other threads by releasing a semaphore.
 * 
 * @author Timon Hackenjos
 *
 */
public class Waker implements Runnable {
  private final Semaphore semaphore = new Semaphore(0);
  private final Thread thread;
  private final Semaphore release;
  private Long sleep;
  private volatile boolean sleeping = false;

  public Waker(Semaphore release) {
    this(release, null);
  }

  public Waker(Semaphore release, ThreadGroup group) {
    this.release = release;
    this.thread = new Thread(group, this);
  }

  public void start() {
    thread.start();
  }

  public void stop() {
    thread.interrupt();
  }

  @Override
  public void run() {

    while (!Thread.interrupted()) {
      try {
        semaphore.acquire();

        sleeping = true;

        Thread.sleep(sleep);

        sleeping = false;

        // wake
        release.release();
      } catch (InterruptedException e) {
        sleeping = false;
        thread.interrupt();
      }
    }
  }

  /**
   * Instructs the thread to release the semaphore after the specified time.
   * Does nothing if another request to release the semaphore is still pending.
   * 
   * @param millis The time in milliseconds to wait until the semaphore should be released.
   */
  public void wake(long millis) {
    if (millis < 0) {
      throw new IllegalArgumentException();
    }

    if (sleeping) {
      return;
    }
    sleep = millis;

    semaphore.release();
  }
}
