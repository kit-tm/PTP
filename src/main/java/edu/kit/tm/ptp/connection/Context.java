package edu.kit.tm.ptp.connection;

import edu.kit.tm.ptp.Identifier;
import edu.kit.tm.ptp.channels.MessageChannel;

/**
 * Manages the current state of a MessageChannel.
 * Part of the state pattern.
 * 
 * @author Timon Hackenjos
 *
 */

public class Context {
  private AbstractState state;
  private AbstractState concreteInit;
  private AbstractState concreteConnect;
  private AbstractState concreteConnectSOCKS;
  private AbstractState concreteConnected;
  private AbstractState concreteAuthenticated;
  private AbstractState concreteClosed;
  private ConnectionManager manager;
  
  /**
   * Contructs a new ChannelContext.
   * 
   * @param manager The ConnectionManager to use.
   */
  public Context(ConnectionManager manager) {
    this.manager = manager;
    
    concreteInit = new StateInit(this);
    concreteConnect = new StateConnect(this);
    concreteConnectSOCKS = new StateConnectSOCKS(this);
    concreteConnected = new StateConnected(this);
    concreteAuthenticated = new StateAuthenticated(this);
    concreteClosed = new StateClosed(this);
    
    state = concreteInit;
  }
  
  public void setState(AbstractState state) {
    this.state = state;
  }
  
  public void opened(MessageChannel channel) {
    state.opened(channel);
  }
  
  public void authenticate(MessageChannel channel) {
    state.authenticate(channel);
  }
  
  public void authenticated(MessageChannel channel, Identifier identifier) {
    state.authenticated(channel, identifier);
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
  
  public AbstractState getConcreteInit() {
    return concreteInit;
  }

  public AbstractState getConcreteConnect() {
    return concreteConnect;
  }

  public AbstractState getConcreteConnectSOCKS() {
    return concreteConnectSOCKS;
  }

  public AbstractState getConcreteConnected() {
    return concreteConnected;
  }

  public AbstractState getConcreteAuthenticated() {
    return concreteAuthenticated;
  }

  public AbstractState getConcreteClosed() {
    return concreteClosed;
  }
}
