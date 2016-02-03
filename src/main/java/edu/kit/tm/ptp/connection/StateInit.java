package edu.kit.tm.ptp.connection;

import edu.kit.tm.ptp.Identifier;
import edu.kit.tm.ptp.auth.Authenticator;
import edu.kit.tm.ptp.auth.DummyAuthenticator;
import edu.kit.tm.ptp.channels.MessageChannel;
import edu.kit.tm.ptp.utility.Constants;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.logging.Level;

/**
 * Initial state of a message channel.
 * State transition is triggered by an incoming connection
 * or an outgoing connection attempt.
 * Part of the state pattern.
 * 
 * @author Timon Hackenjos
 *
 */

public class StateInit extends AbstractState {
  public StateInit(Context context) {
    super(context);
  }

  @Override
  public void open(MessageChannel channel) {
    ConnectionManager manager = context.getConnectionManager();
    // Incoming connection
    try {
      manager.logger.log(Level.INFO,
          "Received new connection from " + channel.getChannel().getRemoteAddress().toString());
    } catch (IOException ioe) {
      manager.logger.log(Level.WARNING, "Failed to get remote address of new channel", ioe);
      close(channel);
      return;
    }
    
    context.setState(context.getConcreteConnected());

    Authenticator auth = new DummyAuthenticator(manager, channel, manager.serializer);
    auth.authenticate(manager.localIdentifier);
    
    try {
      manager.channelManager.addChannel(channel);
    } catch (ClosedChannelException e) {
      manager.logger.log(Level.WARNING, "Channel was closed while adding channel to ChannelManager",
          e);
      close(channel);
      return;
    }
  }

  @Override
  public boolean sendMessage(MessageAttempt attempt) {
    ConnectionManager manager = context.getConnectionManager();

    Identifier identifier = attempt.getDestination();
    MessageChannel channel = null;

    manager.logger.log(Level.INFO, "Connection to destination " + identifier + " is closed");
    if (manager.lastTry.get(identifier) == null || System.currentTimeMillis()
        - manager.lastTry.get(identifier) >= Constants.connectInterval) {
      manager.logger.log(Level.INFO, "Opening new connection to destination " + identifier);
      manager.lastTry.put(identifier, System.currentTimeMillis());
      try {
        channel = manager.connect(identifier);

        manager.identifierMap.put(identifier, channel);
        manager.channelMap.put(channel, identifier);
        manager.channelContexts.put(channel, context);
        
        context.setState(context.getConcreteConnect());
      } catch (IOException ioe) {
        manager.logger.log(Level.WARNING,
            "Error while trying to open a new connection to " + identifier, ioe);
      }
    }
    
    return false;
  }

}