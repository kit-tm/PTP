package edu.kit.tm.ptp.connection;

/**
 * State of a closed channel.
 * 
 * @author Timon Hackenjos
 *
 */

public class Closed extends ChannelState {
  public Closed(ChannelContext context) {
    super(context);
  }

}
