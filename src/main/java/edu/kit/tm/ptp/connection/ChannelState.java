package edu.kit.tm.ptp.connection;

import edu.kit.tm.ptp.Identifier;
import edu.kit.tm.ptp.MessageAttempt;
import edu.kit.tm.ptp.channels.MessageChannel;

import java.io.IOException;
import java.util.logging.Level;

/**
 * Abstract state of a message channel.
 * Part of the state pattern.
 * 
 * @author Timon Hackenjos
 *
 */
public abstract class ChannelState {
  protected ChannelContext context;

  public ChannelState(ChannelContext context) {
    this.context = context;
  }

  public void open(MessageChannel channel) {
    throw new IllegalStateException();
  }

  public void authenticate(MessageChannel channel, Identifier identifier) {
    throw new IllegalStateException();
  }

  /**
   * Return true if the message has been added to a channel.
   */
  public boolean sendMessage(MessageAttempt attempt) {
    return false;
  }

  public void close(MessageChannel channel) {
    ConnectionManager manager = context.getConnectionManager();

    manager.channelManager.removeChannel(channel);

    try {
      channel.getChannel().close();
    } catch (IOException e) {
      manager.logger.log(Level.INFO, "Error while trying to close channel");
    }

    Identifier identifier = manager.channelMap.get(channel);

    if (identifier != null) {
      for (MessageAttempt attempt : manager.dispatchedMessages) {
        if (identifier.equals(attempt.getDestination())) {
          manager.messageQueue.add(attempt);
        }
      }
      
      MessageChannel registeredChannel = manager.identifierMap.get(identifier);

      if (registeredChannel != null && registeredChannel.equals(channel)) {
        manager.identifierMap.remove(identifier);
      }
    }

    manager.channelMap.remove(channel);
    manager.channelContexts.remove(channel);

    manager.logger.log(Level.INFO, "Closed connection "
        + (identifier != null ? "to identifier " + identifier.toString() : ""));
    
    context.setState(context.getConcreteClosed());
  }
}
