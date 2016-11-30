package edu.kit.tm.ptp.channels;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

/**
 * Event for removing a channel from a selector.
 *
 * @author Timon Hackenjos
 */
public class ChannelEventRemove extends ChannelEvent {
  private final MessageChannel channel;

  /**
   * Creates an event to remove a registered channel.
   *
   * @param channel The channel to remove.
   */
  public ChannelEventRemove(MessageChannel channel) {
    this.channel = channel;
  }

  @Override
  public void process(Selector selector)  {
    SelectionKey key = channel.getChannel().keyFor(selector);

    if (key != null) {
      key.cancel();
    }
  }
}
