package edu.kit.tm.ptp.channels;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads and writes messages to a SocketChannel.
 * A message is always prepended by it's length.
 *
 * @author Timon Hackenjos
 */

public class MessageChannel {

  private enum State {
    IDLE, LENGTH, DATA, CLOSED
  }

  private static final Logger logger = Logger.getLogger(MessageChannel.class.getName());
  private static final int bufferLength = 1024;
  private static final int maxBufferLength = 1024 * 1024 * 100; // 100MB

  private final ByteBuffer sendLengthBuffer;
  private final ByteBuffer receiveLengthBuffer;
  private final int lenLength = 4;

  private ByteBuffer sendBuffer;
  private ByteBuffer receiveBuffer;
  private State readState = State.LENGTH;
  private State writeState = State.IDLE;
  private int readLength;
  private long currentId;

  protected final SocketChannel channel;
  protected final ChannelChangeListener changeListener;
  protected final ChannelMessageListener messageListener;
  protected final ChannelManager manager;

  /**
   * Initializes a new MessageChannel.
   * 
   * @param channel The SocketChannel to read from and write to.
   * @param manager The ChannelManager to get the ChannelListener from
   *                and register if this channel has data to write.
   */
  public MessageChannel(SocketChannel channel, ChannelManager manager) {
    if (channel == null || manager == null) {
      throw new NullPointerException();
    }

    this.channel = channel;
    this.manager = manager;
    this.changeListener = manager.getChannelListener();
    this.messageListener = manager.getChannelListener();
    
    // Initialize buffers
    receiveBuffer = ByteBuffer.allocate(bufferLength);
    sendLengthBuffer = ByteBuffer.allocate(lenLength);
    receiveLengthBuffer = ByteBuffer.allocate(lenLength);
  }

  /**
   * Reads data from the channel.
   * Possibly needs to be called several times to read a whole message.
   * Informs the ChannelListener when a whole message has been read.
   */
  public synchronized void read() {
    try {
      int read;
      switch (readState) {
        case LENGTH:

          read = channel.read(receiveLengthBuffer);

          if (read == -1) {
            logger.log(Level.INFO, "Reading reached end of stream");
            closeChannel();
            return;
          }

          if (!receiveLengthBuffer.hasRemaining()) {
            receiveLengthBuffer.flip();
            readLength = receiveLengthBuffer.getInt();
            receiveLengthBuffer.clear();
            
            if (readLength > maxBufferLength) {
              logger.log(Level.WARNING, "Read length exceeded maximum buffer size");
              closeChannel();
              return;
            }
            
            if (readLength == 0) {
              messageListener.messageReceived(new byte[0], this);
              return;
            }
            
            receiveBuffer = ByteBuffer.allocate(readLength);
            readState = State.DATA;
          }

          break;
        case DATA:
          read = channel.read(receiveBuffer);

          if (read == -1) {
            logger.log(Level.INFO, "Reading reached end of stream");
            closeChannel();
            return;
          }

          if (!receiveBuffer.hasRemaining()) {
            byte[] data = receiveBuffer.array();

            messageListener.messageReceived(data, this);
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

  /**
   * Closes the channel.
   */
  protected synchronized void closeChannel() {
    readState = State.CLOSED;
    writeState = State.CLOSED;

    try {
      channel.close();
    } catch (IOException e) {
      logger.log(Level.INFO, "Failed to close channel " + e.getMessage());
    }

    changeListener.channelClosed(this);
  }

  /**
   * Writes a previously added Message to the channel.
   * Needs possibly to be called several times to write a whole message.
   * Informs the ChannelListener when a whole message has been written.
   */
  public synchronized void write() {
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
            messageListener.messageSent(currentId, this);
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
      logger.log(Level.WARNING, "Caught exception while writing: " + ioe.getMessage());
      closeChannel();
    }
  }

  /**
   * Adds a message to write to the MessageChannel.
   * A MessageChannel can only have one message to write at a time.
   * It's only allowed to call the method again, when the
   * ChannelListener is informed, that the last message was
   * sent successfully.
   * 
   * @param data The bytes to send.
   * @param id The id to use when informing the ChannelListener about a sent message.
   * @return True if the channel was idle and the message has been added successfully.
   */
  public synchronized boolean addMessage(byte[] data, long id) {
    if (writeState != State.IDLE) {
      logger.log(Level.INFO, "MessageChannel is busy. Can't add message " + id + ".");
      return false;
    }

    sendLengthBuffer.putInt(data.length);
    sendLengthBuffer.flip();
    sendBuffer = ByteBuffer.wrap(data);

    currentId = id;
    writeState = State.LENGTH;
    manager.registerWrite(this, true);
    return true;
  }

  public SocketChannel getChannel() {
    return channel;
  }
}
