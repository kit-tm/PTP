package edu.kit.tm.ptp.channels;

import java.nio.channels.SocketChannel;

public class SOCKSChannel extends MessageChannel {
  /*public SOCKSChannel(SocketChannel channel, ChannelManager manager) {
    super(channel, manager);
  }*/
  
  public SOCKSChannel(MessageChannel messageChannel, ChannelManager manager) {
    super(messageChannel.getChannel(), manager);
  }
 
  public void connetThroughSOCKS(String host, int port) {
    
  }
}
