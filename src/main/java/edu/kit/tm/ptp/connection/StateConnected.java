package edu.kit.tm.ptp.connection;

import edu.kit.tm.ptp.Identifier;
import edu.kit.tm.ptp.auth.Authenticator;
import edu.kit.tm.ptp.channels.MessageChannel;

import java.util.logging.Level;

/**
 * State of a channel which is connected to a hidden service. A state transition is triggered by a
 * successful authentication.
 * 
 * @author Timon Hackenjos
 *
 */

public class StateConnected extends AbstractState {
  private Authenticator auth = null;

  public StateConnected(Context context) {
    super(context);
  }

  @Override
  public void authenticated(MessageChannel channel, Identifier identifier) {
    ConnectionManager manager = context.getConnectionManager();

    if (identifier == null) {
      // Auth wasn't successfull
      identifier = manager.channelMap.get(channel);

      if (identifier == null) {
        manager.logger.log(Level.WARNING,
            "Authentication attempt on non-registered channel failed");
      } else {
        manager.logger.log(Level.INFO, "Authenticating connection to " + identifier + " failed");
      }

      close(channel);
    } else {
      // Auth was successfull
      manager.logger.log(Level.INFO,
          "Connection to " + identifier + " has been authenticated successfully");

      context.setState(context.getConcreteAuthenticated());

      MessageChannel other = manager.identifierMap.get(identifier);

      if (other != null && !other.equals(channel) && !identifier.equals(manager.localIdentifier)) {
        manager.logger.log(Level.WARNING,
            "Another connection to identifier is already open. Closing the old connection.");
        manager.channelClosed(other);
      }

      manager.identifierMap.put(identifier, channel);
      manager.channelMap.put(channel, identifier);
    }
  }

  @Override
  public void authenticate(MessageChannel channel) {
    ConnectionManager manager = context.getConnectionManager();

    Identifier other = manager.channelMap.get(channel);

    auth =
        manager.authFactory.createInstance(manager, manager, channel);
    if (other != null) {
      auth.authenticate(manager.localIdentifier, other);
    } else {
      auth.authenticate(manager.localIdentifier);
    }

    // Enable reading from channel
    manager.channelManager.registerRead(channel, true);
  }

  @Override
  public void messageReceived(byte[] data, MessageChannel source) {
    ConnectionManager manager = context.getConnectionManager();

    if (auth == null) {
      manager.logger.log(Level.WARNING, "Received auth message before authentication started.");
      return;
    }

    auth.messageReceived(data, source);
  }

  @Override
  public void messageSent(long id, MessageChannel destination) {
    ConnectionManager manager = context.getConnectionManager();

    if (auth == null) {
      manager.logger.log(Level.WARNING, "Message sent before authentication started.");
      return;
    }

    auth.messageSent(id, destination);
  }

}
