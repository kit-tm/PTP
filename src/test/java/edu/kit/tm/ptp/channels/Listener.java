package edu.kit.tm.ptp.channels;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public   class Listener implements ChannelListener {
  public MessageChannel passedChannel;
  public AtomicInteger conOpen = new AtomicInteger(0);
  public AtomicInteger conClosed = new AtomicInteger(0);
  public AtomicInteger other = new AtomicInteger(0);
  public AtomicInteger read = new AtomicInteger(0);
  public AtomicInteger write = new AtomicInteger(0);
  public List<MessageChannel> passedChannels = new LinkedList<MessageChannel>();
  public byte[] passedBytes;
  public long passedId;
  public MessageChannel destination;
  public MessageChannel source;

  @Override
  public void messageSent(long id, MessageChannel destination) {
    other.incrementAndGet();
    write.incrementAndGet();
    passedId = id;
    this.destination = destination;
  }

  @Override
  public void messageReceived(byte[] data, MessageChannel source) {
    other.incrementAndGet();
    read.incrementAndGet();
    passedBytes = data;
    this.source = source;
  }

  @Override
  public void channelOpened(MessageChannel channel) {
    conOpen.incrementAndGet();
    passedChannel = channel;
    passedChannels.add(channel);
  }

  @Override
  public void channelClosed(MessageChannel channel) {
    conClosed.incrementAndGet();
    passedChannel = channel;
  }

}
