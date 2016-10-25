package edu.kit.tm.ptp.connection;

import edu.kit.tm.ptp.Identifier;
import edu.kit.tm.ptp.ReceiveListener;
import edu.kit.tm.ptp.SendListener;
import edu.kit.tm.ptp.channels.MessageChannel;

import java.util.Iterator;
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
      manager.dispatchedMessages.put(attempt.getId(), attempt);
      return true;
    } else {
      return false;
    }
  }

  @Override
  public void messageReceived(byte[] data, MessageChannel source) {
    ConnectionManager manager = context.getConnectionManager();

    Identifier identifier = manager.channelMap.get(source);

    if (identifier == null) {
      manager.logger.log(Level.WARNING,
          "Received message with size " + data.length + " from unknown channel");
      return;
    }

    manager.logger.log(Level.INFO,
        "Received message from " + identifier + " with size " + data.length);

    ReceiveListener receiveListener = manager.receiveListener;
    if (receiveListener != null) {
      receiveListener.messageReceived(data, identifier);
    } else {
      manager.logger.log(Level.WARNING, "Dropped message because no listener is set.");
    }
  }

  @Override
  public void messageSent(long id, MessageChannel destination) {
    ConnectionManager manager = context.getConnectionManager();

    MessageAttempt attempt = manager.dispatchedMessages.get(id);

    if (attempt == null) {
      manager.logger.log(Level.WARNING, "Unknown message id of sent message " + id);
      throw new IllegalStateException();
    }

    manager.dispatchedMessages.remove(id);

    if (manager.sendListener != null && attempt.isInformSendListener()) {
      manager.sendListener.messageSent(id, attempt.getDestination(), SendListener.State.SUCCESS);
    }

    if (attempt.getTimeout() != -1
        && attempt.getSendTimestamp() + attempt.getTimeout() < System.currentTimeMillis()) {
      manager.logger.log(Level.WARNING,
          "Message with id " + attempt.getId() + " was sent even though the timer expired");
    }

  }
}
