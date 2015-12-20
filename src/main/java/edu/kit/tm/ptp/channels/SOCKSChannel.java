package edu.kit.tm.ptp.channels;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class SOCKSChannel extends MessageChannel {
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

  public void connetThroughSOCKS(String host, int port) {
    if (connected) {
      throw new IllegalStateException();
    }

    socksWriteBuffer = ByteBuffer.allocate(host.getBytes().length + 10);

    // ByteBuffers use Big Endian by Default
    // SOCKS4a
    socksWriteBuffer.put((byte) 0x04);
    socksWriteBuffer.put((byte) 0x01);
    socksWriteBuffer.putShort((short) port);
    socksWriteBuffer.putInt(0x01);
    socksWriteBuffer.put((byte) 0x00);
    socksWriteBuffer.put(host.getBytes());
    socksWriteBuffer.put((byte) 0x00);
    socksWriteBuffer.flip();

    socksReceiveBuffer = ByteBuffer.allocate(8);
  }

  @Override
  public void read() {
    if (connected) {
      super.read();
    } else {
      try {
        int read = channel.read(socksReceiveBuffer);

        if (!socksReceiveBuffer.hasRemaining()) {
          socksReceiveBuffer.flip();

          if (socksReceiveBuffer.get() != 0x0 || socksReceiveBuffer.get() != 0x5a) {
            closeChannel();
            return;
          }

          connected = true;
          listener.channelOpened(this);
        } else if (read == -1) {
          closeChannel();
        }
      } catch (IOException e) {
        closeChannel();
      }
    }
  }

  @Override
  public void write() {
    if (connected) {
      super.write();
    } else {
      try {
        channel.write(socksWriteBuffer);
        if (!socksWriteBuffer.hasRemaining()) {
          manager.registerWrite(this, false);
        }
      } catch (IOException e) {
        closeChannel();
      }
    }
  }

  @Override
  public void addMessage(byte[] data, long id) {
    if (!connected) {
      throw new IllegalStateException();
    }

    super.addMessage(data, id);
  }
}
