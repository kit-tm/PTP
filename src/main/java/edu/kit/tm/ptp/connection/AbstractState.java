package edu.kit.tm.ptp.connection;

import edu.kit.tm.ptp.Identifier;
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
public abstract class AbstractState {
  protected Context context;

  public AbstractState(Context context) {
    this.context = context;
  }

  /**
   * Gets called when an incoming or an outgoing connection has been opened.
   * 
   * @param channel The connection.
   */
  public void open(MessageChannel channel) {
    throw new IllegalStateException();
  }

  /**
   * Gets called when a connection has been authenticated.
   * 
   * @param channel The connection.
   * @param identifier The Identifier supplied by the remote end.
   */
  public void authenticate(MessageChannel channel, Identifier identifier) {
    throw new IllegalStateException();
  }

  /**
   * Try to send the message.
   * Opens a new connection to the destination if necessary.
   * Return true if the message has been added to a channel.
   */
  public boolean sendMessage(MessageAttempt attempt) {
    return false;
  }

  /**
   * Closes a channel and removes it from the configuration.
   * 
   * @param channel The channel to close.
   */
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
