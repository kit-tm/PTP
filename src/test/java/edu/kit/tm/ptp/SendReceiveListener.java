package edu.kit.tm.ptp;

import java.util.concurrent.atomic.AtomicInteger;

public class SendReceiveListener implements SendListener, ReceiveListener {
  public AtomicInteger sent = new AtomicInteger(0);
  public AtomicInteger received = new AtomicInteger(0);
  public Identifier destination;
  public long id;
  public State state;

  @Override
  public void messageReceived(byte[] data, Identifier source) {
    received.incrementAndGet();
  }

  @Override
  public void messageSent(long id, Identifier destination, State state) {
    this.id = id;
    this.destination = destination;
    this.state = state;
    sent.incrementAndGet();
  }
}
