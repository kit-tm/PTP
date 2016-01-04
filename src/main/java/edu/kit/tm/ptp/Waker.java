package edu.kit.tm.ptp;

import java.util.concurrent.Semaphore;

public class Waker implements Runnable {
  private Semaphore semaphore = new Semaphore(0);
  private Thread thread;
  private Semaphore release;
  Long sleep;
  private volatile boolean sleeping = false;

  public Waker(Semaphore release) {
    this.release = release;
  }

  public void start() {
    thread = new Thread(this);
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
