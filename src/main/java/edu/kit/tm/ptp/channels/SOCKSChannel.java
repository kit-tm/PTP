package edu.kit.tm.ptp.channels;

import java.nio.channels.SocketChannel;

public class SOCKSChannel extends MessageChannel {
  public SOCKSChannel(SocketChannel channel) {
    super(channel);
  }
  
  public SOCKSChannel(MessageChannel messageChannel) {
    super(messageChannel.getChannel());
  }
 
  public void connetThroughSOCKS(String host, int port) {
    
  }
}
