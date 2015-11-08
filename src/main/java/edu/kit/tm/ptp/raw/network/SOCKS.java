package edu.kit.tm.ptp.raw.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;


public class SOCKS {

  /* source: the Tor Onion Proxy Library */
  public static Socket socks4aSocketConnection(String networkHost, int networkPort,
      String socksHost, int socksPort, int connectTimeoutMilliseconds) throws IOException {
    Socket socket = new Socket();
    socket.setSoTimeout(connectTimeoutMilliseconds);
    SocketAddress socksAddress = new InetSocketAddress(socksHost, socksPort);
    socket.connect(socksAddress, connectTimeoutMilliseconds);

    DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
    outputStream.write((byte) 0x04);
    outputStream.write((byte) 0x01);
    outputStream.writeShort((short) networkPort);
    outputStream.writeInt(0x01);
    outputStream.write((byte) 0x00);
    outputStream.write(networkHost.getBytes());
    outputStream.write((byte) 0x00);

    DataInputStream inputStream = new DataInputStream(socket.getInputStream());
    byte firstByte = inputStream.readByte();
    byte secondByte = inputStream.readByte();
    if (firstByte != (byte) 0x00 || secondByte != (byte) 0x5a) {
      socket.close();
      throw new IOException("SOCKS4a connect failed, got " + firstByte + " - " + secondByte
          + ", but expected 0x00 - 0x5a");
    }

    inputStream.readShort();
    inputStream.readInt();

    socket.setSoTimeout(0);
    return socket;
  }

}
