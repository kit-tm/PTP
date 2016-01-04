package edu.kit.tm.ptp.channels;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Class.
 *
 * @author Timon Hackenjos
 */

public class MessageChannel {

  private enum State {
    IDLE, LENGTH, DATA, CLOSED
  }

  private ByteBuffer sendBuffer;
  private ByteBuffer sendLengthBuffer;
  private ByteBuffer receiveBuffer;
  private ByteBuffer receiveLengthBuffer;
  protected SocketChannel channel;
  private int bufferLength = 1024;
  private int maxBufferLength = 1024 * 1024 * 100; // 100MB
  protected ChannelListener listener;
  private State readState = State.LENGTH;
  private State writeState = State.IDLE;

  private int readLength;
  private final int lenLength = 4;
  private long currentId;
  private Lock writeLock = new ReentrantLock();
  protected ChannelManager manager;

  public MessageChannel(SocketChannel channel, ChannelManager manager) {
    if (channel == null || manager == null) {
      throw new NullPointerException();
    }

    this.channel = channel;
    this.manager = manager;
    this.listener = manager.getChannelListener();
    // Initialize buffers
    receiveBuffer = ByteBuffer.allocate(bufferLength);
    sendLengthBuffer = ByteBuffer.allocate(lenLength);
    receiveLengthBuffer = ByteBuffer.allocate(lenLength);
  }

  public void read() {
    try {
      int read;
      switch (readState) {
        case LENGTH:

          read = channel.read(receiveLengthBuffer);

          if (read == -1) {
            closeChannel();
            return;
          }

          if (!receiveLengthBuffer.hasRemaining()) {
            receiveLengthBuffer.flip();
            readLength = receiveLengthBuffer.getInt();
            receiveLengthBuffer.clear();
            
            if (readLength > maxBufferLength) {
              // TODO log
              closeChannel();
              return;
            }
            
            receiveBuffer = ByteBuffer.allocate(readLength);
            readState = State.DATA;
          }

          break;
        case DATA:
          read = channel.read(receiveBuffer);
          if (!receiveBuffer.hasRemaining()) {
            byte[] data = receiveBuffer.array();

            listener.messageReceived(data, this);
            readState = State.LENGTH;

            receiveBuffer.clear();
          }
          break;
        default:
          break;

      }

    } catch (IOException ioe) {
      closeChannel();
    }
  }

  protected void closeChannel() {
    readState = State.CLOSED;

    writeLock.lock();
    writeState = State.CLOSED;
    writeLock.unlock();

    try {
      channel.close();
    } catch (IOException e) {
      // TODO log error
    }

    listener.channelClosed(this);
  }

  public void write() {
    // write object length
    // write object
    writeLock.lock();

    try {
      switch (writeState) {
        case LENGTH:

          channel.write(sendLengthBuffer);

          if (!sendLengthBuffer.hasRemaining()) {
            sendLengthBuffer.clear();
            writeState = State.DATA;
          }
          break;
        case DATA:
          channel.write(sendBuffer);

          if (!sendBuffer.hasRemaining()) {
            listener.messageSent(currentId, this);
            writeState = State.IDLE;
          }
          break;
        case IDLE:
          manager.registerWrite(this, false);
          break;
        default:
          break;

      }
    } catch (IOException ioe) {
      closeChannel();
    }

    writeLock.unlock();
  }

  public void addMessage(byte[] data, long id) {
    writeLock.lock();
    if (writeState != State.IDLE) {
      throw new IllegalStateException();
    }
    sendLengthBuffer.putInt(data.length);
    sendLengthBuffer.flip();
    sendBuffer = ByteBuffer.wrap(data);

    currentId = id;
    writeState = State.LENGTH;
    manager.registerWrite(this, true);
    writeLock.unlock();
  }

  public SocketChannel getChannel() {
    return channel;
  }

  public void setChannelListener(ChannelListener listener) {
    this.listener = listener;
  }

  public ChannelListener getChannenListener() {
    return listener;
  }

  public boolean isIdle() {
    return writeState == State.IDLE;
  }
}
