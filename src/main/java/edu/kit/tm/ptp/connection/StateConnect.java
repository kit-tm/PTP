package edu.kit.tm.ptp.connection;

import edu.kit.tm.ptp.Identifier;
import edu.kit.tm.ptp.channels.MessageChannel;
import edu.kit.tm.ptp.channels.SOCKSChannel;

import java.nio.channels.ClosedChannelException;
import java.util.logging.Level;


/**
 * State of a channel which opens an ongoing connection.
 * A state transition is triggered by a successful
 * connect to the tor socks proxy.
 * 
 * @author Timon Hackenjos
 *
 */
public class StateConnect extends AbstractState {
  public StateConnect(Context context) {
    super(context);
  }

  @Override
  public void opened(MessageChannel channel) {
    ConnectionManager manager = context.getConnectionManager();

    Identifier identifier = manager.channelMap.get(channel);

    manager.logger.log(Level.INFO,
        "Trying to connect to " + identifier + " through tor socks proxy");
    
    // remove channel from maps
    manager.identifierMap.remove(identifier);
    manager.channelMap.remove(channel);
    manager.channelContexts.remove(channel);
    
    // create new socks channel
    SOCKSChannel socks = new SOCKSChannel(channel, manager.channelManager);

    try {
      manager.channelManager.addChannel(socks);
      
      manager.identifierMap.put(identifier, socks);
      manager.channelMap.put(socks, identifier);
      manager.channelContexts.put(socks, context);
      
      context.setState(context.getConcreteConnectSOCKS());

      socks.connetThroughSOCKS(identifier.getTorAddress(), manager.hsPort);
    } catch (ClosedChannelException e) {
      manager.logger.log(Level.WARNING, "Channel was closed while adding channel to ChannelManager",
          e);
      close(socks);
      return;
    }

  }
}
