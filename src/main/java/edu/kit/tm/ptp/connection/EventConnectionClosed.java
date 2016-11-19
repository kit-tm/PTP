package edu.kit.tm.ptp.connection;

import edu.kit.tm.ptp.channels.MessageChannel;

import java.util.logging.Level;


/**
 * Class for the event that a connection was closed.
 */

public class EventConnectionClosed extends Event {
  private MessageChannel channel;

  public EventConnectionClosed(ConnectionManager manager, MessageChannel channel) {
    super(manager);

    this.channel = channel;
  }

  @Override
  public boolean process() {
    Context context = manager.channelContexts.get(channel);

    if (context == null) {
      manager.logger.log(Level.INFO, "Channel has been closed already.");
    } else {
      context.close(channel);
    }

    return true;
  }
}
