package edu.kit.tm.ptp.connection;

import edu.kit.tm.ptp.Identifier;

import java.util.logging.Level;


/**
 * Class for the event of setting an hidden service identifier.
 */

public class EventSetIdentifier extends Event {
  private Identifier identifier;

  public EventSetIdentifier(ConnectionManager manager, Identifier identifier) {
    super(manager);

    this.identifier = identifier;
  }

  @Override
  public boolean process() {
    if (identifier == null || !identifier.isValid()) {
      manager.logger.log(Level.WARNING, "Identifier is invalid.");
      return true;
    }

    manager.localIdentifier = identifier;
    manager.logger.log(Level.INFO, "Set local identifier to " + identifier);

    return true;
  }
}
