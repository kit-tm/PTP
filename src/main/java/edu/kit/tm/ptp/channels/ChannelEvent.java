package edu.kit.tm.ptp.channels;

import java.nio.channels.Selector;

/**
 * Abstract class for events that change the registration of a channel.
 *
 * @author Timon Hackenjos
 */
public abstract class ChannelEvent {
  /**
   * Process the event using the supplied selector.
   */
  public abstract void process(Selector selector);
}
