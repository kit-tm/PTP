package edu.kit.tm.ptp.connection;

import edu.kit.tm.ptp.Identifier;
import edu.kit.tm.ptp.channels.MessageChannel;

import java.util.logging.Level;

/**
 * State of a channel which is connected to a hidden service. A state transition is triggered by a
 * successful authentication.
 * 
 * @author Timon Hackenjos
 *
 */

public class Connected extends ChannelState {
  public Connected(ChannelContext context) {
    super(context);
  }

  @Override
  public void authenticate(MessageChannel channel, Identifier identifier) {
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
      context.setState(context.getConcreteAuthenticated());
      // TODO check for other connections to identifier

      MessageChannel other = manager.identifierMap.get(identifier);

      if (other != null && !other.equals(channel)) {
        // Commented out because it prevents sending messages to own identifier
        // manager.channelClosed(channel);
        manager.logger.log(Level.WARNING,
            "Another connection to identifier is open. Overwriting entry in identifierMap");
      }

      manager.identifierMap.put(identifier, channel);
      manager.channelMap.put(channel, identifier);

      manager.logger.log(Level.INFO,
          "Connection to " + identifier + " has been authenticated successfully");
    }
  }

}
