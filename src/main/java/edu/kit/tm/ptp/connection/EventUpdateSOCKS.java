package edu.kit.tm.ptp.connection;

/**
 * Class for the event of updating the SOCKS proxy information.
 */

public class EventUpdateSOCKS extends Event {
  private String socksHost;
  private int socksPort;

  public EventUpdateSOCKS(ConnectionManager manager, String socksHost, int socksPort) {
    super(manager);

    this.socksHost = socksHost;
    this.socksPort = socksPort;
  }

  @Override
  public boolean process() {
    manager.socksPort = socksPort;
    manager.socksHost = socksHost;

    return true;
  }
}
