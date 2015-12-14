package edu.kit.tm.ptp.channels;

import java.io.IOException;
import java.nio.ByteBuffer;

public class SOCKSChannel extends MessageChannel {
  private boolean connected;
  private ByteBuffer socksReceiveBuffer;
  private ByteBuffer socksWriteBuffer;

  public SOCKSChannel(MessageChannel messageChannel, ChannelManager manager) {
    super(messageChannel.getChannel(), manager);
    connected = false;
  }

  public void connetThroughSOCKS(String host, int port) {
    if (connected) {
      throw new IllegalStateException();
    }

    socksWriteBuffer = ByteBuffer.allocate(host.getBytes().length + 10);

    // ByteBuffers use Big Endian by Default
    // SOCKS4a
    socksWriteBuffer.put(new byte[] {0x04, 0x01});
    socksWriteBuffer.putShort((short) port);
    socksWriteBuffer.put(new byte[] {0x0, 0x0, 0x0, 0x01, 0x0});
    socksWriteBuffer.put(host.getBytes());
    socksWriteBuffer.put(new byte[] {0x0});
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
        
        if (read == -1) {
          closeChannel();
          return;
        }

        if (!socksReceiveBuffer.hasRemaining()) {
          socksReceiveBuffer.flip();

          if (socksReceiveBuffer.get() != 0x0 || socksReceiveBuffer.get() != 0x5a) {
            closeChannel();
            return;
          }
          
          connected = true;
          manager.getChannelListener().channelOpened(this);
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
