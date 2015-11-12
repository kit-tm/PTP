package edu.kit.tm.ptp.raw.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketAddress;


public class SOCKS {
  /**
   * Creates a Socket using the specified SOCKS Proxy and connects it to the specified server.
   * 
   * @param networkHost Hostname of the Server to connect to.
   * @param networkPort Port of the Server to connect to.
   * @param socksHost Hostname of the SOCKS Proxy.
   * @param socksPort Port of the SOCKS Proxy.
   * @param connectTimeoutMilliseconds Timeout to use for the connection attempt.
   * @return The connected Socket.
   * @throws IOException If an error occurs while connecting.
   */
  public static Socket connectThroughSOCKS(String networkHost, int networkPort,
      String socksHost, int socksPort, int connectTimeoutMilliseconds) throws IOException {
    SocketAddress proxyAddress = InetSocketAddress.createUnresolved(socksHost, socksPort);

    Socket socket = new Socket(new Proxy(Proxy.Type.SOCKS, proxyAddress));
    socket.setSoTimeout(connectTimeoutMilliseconds);

    SocketAddress sa = InetSocketAddress.createUnresolved(networkHost, networkPort);
    socket.connect(sa, connectTimeoutMilliseconds);
    
    socket.setSoTimeout(0);
    return socket;
  }

}
