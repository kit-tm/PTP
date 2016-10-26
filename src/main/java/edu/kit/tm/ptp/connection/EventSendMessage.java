package edu.kit.tm.ptp.connection;

import edu.kit.tm.ptp.Identifier;
import edu.kit.tm.ptp.SendListener;
import edu.kit.tm.ptp.channels.MessageChannel;

/**
 * Class for the event that a message should be sent.
 */

public class EventSendMessage extends Event {
  private  MessageAttempt attempt;

  public EventSendMessage(ConnectionManager manager, MessageAttempt attempt) {
    super(manager);

    this.attempt = attempt;
  }

  @Override
  public boolean process() {
    Identifier identifier = attempt.getDestination();

    // Check if identifier is valid
    if (!identifier.isValid()) {
      manager.sendListener.messageSent(attempt.getId(), identifier, SendListener.State.INVALID_DESTINATION);
      return true;
    }

    // Check timeout of message
    if (attempt.getTimeout() != -1
        && System.currentTimeMillis() - attempt.getSendTimestamp() >= attempt.getTimeout()) {
      if (attempt.isInformSendListener()) {
        manager.sendListener.messageSent(attempt.getId(), attempt.getDestination(), SendListener.State.TIMEOUT);
      }
      return true;
    }

    MessageChannel channel = manager.identifierMap.get(identifier);
    Context context = manager.channelContexts.get(channel);

    if (context == null) {
      // No channel exists for the destination yet
      context = new Context(manager);
    }

    return context.sendMessage(attempt);
  }
}
