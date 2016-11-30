package edu.kit.tm.ptp.channels;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Event for changing the interestOps of a channel at a selector.
 *
 * @author Timon Hackenjos
 */
public class ChannelEventSetInterestOps extends ChannelEvent {
  private final MessageChannel channel;
  private final boolean enable;
  private final int operation;
  private static final Logger logger = Logger.getLogger(ChannelEventSetInterestOps.class.getName());

  /**
   * Creates an event to change the interest set of operations of a registered channel.
   *
   * @param channel The registered channel.
   * @param enable True if the operation should be in the interest set.
   * @param operation The operation to add/remove.
   */
  public ChannelEventSetInterestOps(MessageChannel channel, boolean enable, int operation) {
    this.channel = channel;
    this.enable = enable;
    this.operation = operation;
  }

  @Override
  public void process(Selector selector) {
    SelectionKey key = channel.getChannel().keyFor(selector);

    if (key == null) {
      // Can happen for incoming connections
      logger.log(Level.INFO, "Unregistered channel tries to register operation.");
      return;
    }

    if (!key.isValid()) {
      logger.log(Level.INFO, "Closed or unregistered channel tries to register operation.");
      return;
    }

    if (enable) {
      key.interestOps(key.interestOps() | operation);
    } else {
      key.interestOps(key.interestOps() & (~operation));
    }
  }
}
