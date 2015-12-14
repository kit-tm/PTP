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
  private int bufferLength;
  protected ChannelManager manager;
  private State readState = State.LENGTH;
  private State writeState = State.IDLE;

  private int readLength;
  private final int lenLength = 4;
  private long currentId;
  private Lock writeLock = new ReentrantLock();

  public MessageChannel(SocketChannel channel, ChannelManager manager) {
    if (channel == null || manager == null) {
      throw new NullPointerException();
    }

    this.channel = channel;
    this.manager = manager;
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
            receiveBuffer = ByteBuffer.allocate(readLength);
            readState = State.DATA;
          }

          break;
        case DATA:
          read = channel.read(receiveBuffer);
          if (!receiveBuffer.hasRemaining()) {
            byte[] data = receiveBuffer.array();

            manager.getChannelListener().messageReceived(data, this);
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
    
    manager.getChannelListener().channelClosed(this);
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
            manager.getChannelListener().messageSent(currentId, this);
            writeState = State.IDLE;
          }
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
    sendBuffer = ByteBuffer.wrap(data);
    
    currentId = id;
    writeState = State.LENGTH;
    writeLock.unlock();
  }

  public SocketChannel getChannel() {
    return channel;
  }
}
