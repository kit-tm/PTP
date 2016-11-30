package edu.kit.tm.ptp.channels;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.Selector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Event for registering a channel at a selector.
 *
 * @author Timon Hackenjos
 */
public class ChannelEventRegister extends ChannelEvent {
  private final int interestOps;
  private final SelectableChannel channel;
  private final Object attachement;
  private static final Logger logger = Logger.getLogger(ChannelEventSetInterestOps.class.getName());

  /**
   * Creates an event to register a channel.
   *
   * @param interestOps The operations to register for.
   * @param channel The channel to register.
   * @param attach The object to attach to the channel.
   */
  public ChannelEventRegister(int interestOps, SelectableChannel channel, Object attach) {
    this.interestOps = interestOps;
    this.channel = channel;
    this.attachement = attach;
  }

  @Override
  public void process(Selector selector) {
    try {
      channel.register(selector, interestOps, attachement);
    } catch (ClosedChannelException e) {
      logger.log(Level.INFO, "Channel to be registered is already closed.");
    }
  }
}
