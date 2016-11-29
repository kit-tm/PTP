package edu.kit.tm.ptp.channels;

import edu.kit.tm.ptp.utility.Constants;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MessageChannel which allows to connect through a SOCKS proxy.
 * 
 * @author Timon Hackenjos
 *
 */
public class SOCKSChannel extends MessageChannel {
  private static final Logger logger = Logger.getLogger(SOCKSChannel.class.getName());
  private boolean connected;
  private ByteBuffer socksReceiveBuffer;
  private ByteBuffer socksWriteBuffer;

  public SOCKSChannel(MessageChannel messageChannel, ChannelManager manager) {
    super(messageChannel.getChannel(), manager);
    connected = false;
  }

  public SOCKSChannel(SocketChannel channel, ChannelManager manager) {
    super(channel, manager);
    connected = false;
  }

  /**
   * Opens a connection through a SOCKS proxy. The MessageChannel needs to be connected to the SOCKS
   * proxy already. Informs the ChannelListener about a sucessfull connection by calling
   * channelOpenend().
   * 
   * @param host The host to connect to.
   * @param port The port to connect to.
   */
  public synchronized void connectThroughSOCKS(String host, int port) {
    if (connected) {
      logger.log(Level.SEVERE, "A connection through the proxy has already been established.");
      throw new IllegalStateException();
    }
    byte[] hostBytes;
    
    try {
      hostBytes = host.getBytes(Constants.charset);
    } catch (UnsupportedEncodingException e) {
      logger.log(Level.WARNING, "Failed to encode host string using " + Constants.charset);
      closeChannel();
      return;
    }

    socksWriteBuffer = ByteBuffer.allocate(hostBytes.length + 10);

    // ByteBuffers use Big Endian by Default
    // SOCKS4a
    socksWriteBuffer.put((byte) 0x04);
    socksWriteBuffer.put((byte) 0x01);
    socksWriteBuffer.putShort((short) port);
    socksWriteBuffer.putInt(0x01);
    socksWriteBuffer.put((byte) 0x00);
    socksWriteBuffer.put(hostBytes);
    socksWriteBuffer.put((byte) 0x00);
    socksWriteBuffer.flip();

    socksReceiveBuffer = ByteBuffer.allocate(8);
    
    manager.registerRead(this, true);
    manager.registerWrite(this, true);
  }

  @Override
  public synchronized void read() {
    if (connected) {
      super.read();
    } else {
      try {
        int read = channel.read(socksReceiveBuffer);

        if (!socksReceiveBuffer.hasRemaining()) {
          socksReceiveBuffer.flip();

          byte nullbyte = socksReceiveBuffer.get();
          byte status = socksReceiveBuffer.get();

          if (nullbyte != 0x0 || status != 0x5a) {
            logger.log(Level.INFO, "SOCKS proxy rejected request: " + nullbyte + " " + status);
            closeChannel();
            return;
          }

          connected = true;
          changeListener.channelOpened(this);
          
          // Avoid to lose authentication message 
          manager.registerRead(this, false);
        } else if (read == -1) {
          logger.log(Level.WARNING, "Reached end of stream while waiting for answer from proxy.");
          closeChannel();
        }
      } catch (IOException e) {
        logger.log(Level.WARNING,
            "Caught exception while reading answer from proxy: " + e.getMessage());
        closeChannel();
      }
    }
  }

  @Override
  public synchronized void write() {
    if (connected) {
      super.write();
    } else {
      try {
        channel.write(socksWriteBuffer);
        if (!socksWriteBuffer.hasRemaining()) {
          manager.registerWrite(this, false);
        }
      } catch (IOException e) {
        logger.log(Level.WARNING,
            "Caught exception while writing request to proxy: " + e.getMessage());
        closeChannel();
      }
    }
  }

  /**
   * It's not allowed to call this method while it establishes a connection through the SOCKS proxy.
   */
  @Override
  public synchronized boolean addMessage(byte[] data, long id) {
    if (!connected) {
      logger.log(Level.WARNING, "Tried to add message to an unconnected SOCKSChannel.");
      return false;
    }

    return super.addMessage(data, id);
  }
}
