package edu.kit.tm.ptp;

import java.util.concurrent.atomic.AtomicInteger;

public class SendReceiveListener implements SendListener, ReceiveListener {
  public AtomicInteger sent = new AtomicInteger(0);
  public AtomicInteger received = new AtomicInteger(0);
  private Identifier destination;
  private long id;
  private State state;

  @Override
  public void messageReceived(byte[] data, Identifier source) {
    received.incrementAndGet();
  }

  @Override
  public synchronized void messageSent(long id, Identifier destination, State state) {
    this.id = id;
    this.destination = destination;
    this.state = state;
    sent.incrementAndGet();
  }

  public synchronized Identifier getDestination() {
    return destination;
  }

  public synchronized long getId() {
    return id;
  }

  public synchronized State getState() {
    return state;
  }
}
