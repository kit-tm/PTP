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
    passedId = id;
    this.destination = destination;
    other.incrementAndGet();
    write.incrementAndGet();
  }

  @Override
  public void messageReceived(byte[] data, MessageChannel source) {
    passedBytes = data;
    this.source = source;
    other.incrementAndGet();
    read.incrementAndGet();
  }

  @Override
  public void channelOpened(MessageChannel channel) {
    System.out.println("Channel opened");
    passedChannel = channel;
    passedChannels.add(channel);
    conOpen.incrementAndGet();
  }

  @Override
  public void channelClosed(MessageChannel channel) {
    passedChannel = channel;
    conClosed.incrementAndGet();
  }

}
