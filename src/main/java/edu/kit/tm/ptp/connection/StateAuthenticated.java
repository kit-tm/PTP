package edu.kit.tm.ptp.connection;

import edu.kit.tm.ptp.Identifier;
import edu.kit.tm.ptp.channels.MessageChannel;

import java.util.logging.Level;

/**
 * State of a channel which is connected and whose
 * remote end has been authenticated successfully.
 * 
 * @author Timon Hackenjos
 *
 */

public class StateAuthenticated extends StateConnected {
  public StateAuthenticated(Context context) {
    super(context);
  }
  
  @Override
  public boolean sendMessage(MessageAttempt attempt) {
    ConnectionManager manager = context.getConnectionManager();
    
    Identifier identifier = attempt.getDestination();
    MessageChannel channel = manager.identifierMap.get(identifier);
    
    manager.logger.log(Level.INFO,
        "Sending message with id " + attempt.getId() + " to " + attempt.getDestination());

    if (channel == null) {
      throw new IllegalStateException();
    }

    if (channel.isIdle()) {
      channel.addMessage(attempt.getData(), attempt.getId());
      manager.dispatchedMessages.add(attempt);
      return true;
    } else {
      return false;
    }
  }
}
