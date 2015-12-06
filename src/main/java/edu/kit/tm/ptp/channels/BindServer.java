package edu.kit.tm.ptp.channels;

import edu.kit.tm.ptp.utility.Constants;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class BindServer implements Runnable {
  private ServerSocket serverSocket;
  private ChannelManager manager;
  
  public BindServer(ChannelManager manager) {
    this.manager = manager;
  }
  
  public void run() {
    try {
      serverSocket = new ServerSocket(Constants.anyport);
      Socket client;
      
      while(!Thread.interrupted()) {
        client = serverSocket.accept();
        manager.addChannel(client.getChannel());
      }
      
      serverSocket.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  public int getPort() {
    return serverSocket.getLocalPort();
  }
}
