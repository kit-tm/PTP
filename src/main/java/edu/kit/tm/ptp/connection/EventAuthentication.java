package edu.kit.tm.ptp.connection;

import edu.kit.tm.ptp.Identifier;
import edu.kit.tm.ptp.channels.MessageChannel;

/**
 * Class for a the event of a successfull or unsucessfull authentication.
 */

public class EventAuthentication extends Event {
  private MessageChannel channel;
  private Identifier identifier;

  public EventAuthentication(ConnectionManager manager, MessageChannel channel,
                             Identifier identifier) {
    super(manager);

    this.channel = channel;
    this.identifier = identifier;
  }

  @Override
  public boolean process() {
    Context context = manager.channelContexts.get(channel);

    context.authenticated(channel, identifier);
    return true;
  }
}
