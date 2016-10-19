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
        manager.channelClosed(other);
        manager.logger.log(Level.WARNING,
            "Another connection to identifier is open. Overwriting entry in identifierMap");
      }

      manager.identifierMap.put(identifier, channel);
      manager.channelMap.put(channel, identifier);
    }
  }

  @Override
  public void authenticate(MessageChannel channel) {
    ConnectionManager manager = context.getConnectionManager();

    Identifier other = manager.channelMap.get(channel);

    Authenticator auth =
        manager.authFactory.createInstance(manager, manager, channel);
    if (other != null) {
      auth.authenticate(manager.localIdentifier, other);
    } else {
      auth.authenticate(manager.localIdentifier);
    }

    // Enable reading from channel
    manager.channelManager.registerRead(channel, true);
  }

}
