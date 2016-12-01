package edu.kit.tm.ptp.utility;

import edu.kit.tm.ptp.channels.ChannelListener;
import edu.kit.tm.ptp.channels.MessageChannel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Listener implements ChannelListener {
  public AtomicInteger conOpen = new AtomicInteger(0);
  public AtomicInteger conClosed = new AtomicInteger(0);
  public AtomicInteger other = new AtomicInteger(0);
  public AtomicInteger read = new AtomicInteger(0);
  public AtomicInteger write = new AtomicInteger(0);
  private MessageChannel passedChannel;
  private byte[] passedBytes;
  private long passedId;
  private MessageChannel destination;
  private MessageChannel source;

  @Override
  public synchronized void messageSent(long id, MessageChannel destination) {
    passedId = id;
    this.destination = destination;
    other.incrementAndGet();
    write.incrementAndGet();
  }

  @Override
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "MessageChannel uses"
      + "a new buffer for each message and doesn't change old buffers.")
  public synchronized void messageReceived(byte[] data, MessageChannel source) {
    passedBytes = data;
    this.source = source;
    other.incrementAndGet();
    read.incrementAndGet();
  }

  @Override
  public synchronized void channelOpened(MessageChannel channel) {
    passedChannel = channel;
    conOpen.incrementAndGet();
  }

  @Override
  public synchronized void channelClosed(MessageChannel channel) {
    passedChannel = channel;
    conClosed.incrementAndGet();
  }

  public synchronized MessageChannel getPassedChannel() {
    return passedChannel;
  }

  public synchronized byte[] getPassedBytes() {
    return passedBytes;
  }

  public synchronized long getPassedId() {
    return passedId;
  }

  public synchronized MessageChannel getDestination() {
    return destination;
  }

  public synchronized MessageChannel getSource() {
    return source;
  }
}
