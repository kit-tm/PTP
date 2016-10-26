package edu.kit.tm.ptp.connection;

import edu.kit.tm.ptp.channels.MessageChannel;

/**
 * Class for the event of a new incoming connection.
 */

public class EventConnectionOpened extends Event {
  private MessageChannel channel;

  public EventConnectionOpened(ConnectionManager manager, MessageChannel channel) {
    super(manager);
    this.channel = channel;
  }

  @Override
  public boolean process() {
    Context context = manager.channelContexts.get(channel);

    // check if a context object already exists
    if (context == null) {
      // if it's an incoming connection there is no context object
      // create one
      context = new Context(manager);
      manager.channelContexts.put(channel, context);
    }

    context.opened(channel);
    return true;
  }
}
