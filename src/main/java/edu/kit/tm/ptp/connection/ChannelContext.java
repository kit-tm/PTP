package edu.kit.tm.ptp.connection;

import edu.kit.tm.ptp.Identifier;
import edu.kit.tm.ptp.MessageAttempt;
import edu.kit.tm.ptp.channels.MessageChannel;

/**
 * Manages the current state of a MessageChannel.
 * Part of the state pattern.
 * 
 * @author Timon Hackenjos
 *
 */

public class ChannelContext {
  private ChannelState state;
  private ChannelState concreteInit;
  private ChannelState concreteConnect;
  private ChannelState concreteConnectSOCKS;
  private ChannelState concreteConnected;
  private ChannelState concreteAuthenticated;
  private ChannelState concreteClosed;
  private ConnectionManager manager;
  
  public ChannelContext(ConnectionManager manager) {
    this.manager = manager;
    
    concreteInit = new Init(this);
    concreteConnect = new Connect(this);
    concreteConnectSOCKS = new ConnectSOCKS(this);
    concreteConnected = new Connected(this);
    concreteAuthenticated = new Authenticated(this);
    concreteClosed = new Closed(this);
    
    state = concreteInit;
  }
  
  public void setState(ChannelState state) {
    this.state = state;
  }
  
  public void open(MessageChannel channel) {
    state.open(channel);
  }
  
  public void authenticate(MessageChannel channel, Identifier identifier) {
    state.authenticate(channel, identifier);
  }
  
  public void close(MessageChannel channel) {
    state.close(channel);
  }
  
  public boolean sendMessage(MessageAttempt attempt) {
    return state.sendMessage(attempt);
  }
  
  public ConnectionManager getConnectionManager() {
    return manager;
  }
  
  public ChannelState getConcreteInit() {
    return concreteInit;
  }

  public ChannelState getConcreteConnect() {
    return concreteConnect;
  }

  public ChannelState getConcreteConnectSOCKS() {
    return concreteConnectSOCKS;
  }

  public ChannelState getConcreteConnected() {
    return concreteConnected;
  }

  public ChannelState getConcreteAuthenticated() {
    return concreteAuthenticated;
  }

  public ChannelState getConcreteClosed() {
    return concreteClosed;
  }
}
