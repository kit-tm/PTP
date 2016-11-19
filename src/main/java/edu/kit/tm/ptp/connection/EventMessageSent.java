package edu.kit.tm.ptp.connection;

import edu.kit.tm.ptp.channels.MessageChannel;

import java.util.logging.Level;

/**
 * Class for the event that a message was sent successfully.
 */

public class EventMessageSent extends Event {
  private long id;
  private MessageChannel destination;

  public EventMessageSent(ConnectionManager manager, long id, MessageChannel destination) {
    super(manager);
    this.id = id;
    this.destination = destination;
  }

  @Override
  public boolean process() {
    Context context = manager.channelContexts.get(destination);

    if (context == null) {
      manager.logger.log(Level.INFO, "Message sent successfully but channel is already closed.");
    } else {
      context.messageSent(id, destination);
    }

    return true;
  }
}
