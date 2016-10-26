package edu.kit.tm.ptp.connection;

import java.util.logging.Level;

import edu.kit.tm.ptp.channels.MessageChannel;

/**
 * Class for the event that a message was received.
 */

public class EventMessageReceived extends Event {
  private byte[] data;
  private MessageChannel source;

  public EventMessageReceived(ConnectionManager manager, byte[] data, MessageChannel source) {
    super(manager);

    this.data = data;
    this.source = source;
  }

  @Override
  public boolean process() {
    Context context = manager.channelContexts.get(source);

    if (context == null) {
      manager.logger.log(Level.WARNING, "Message received by a channel without a context.");
    } else {
      context.messageReceived(data, source);
    }

    return true;
  }
}
