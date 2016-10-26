package edu.kit.tm.ptp.connection;

/**
 * Abstract class for Events that process themselves.
 */

public abstract class Event {
  protected ConnectionManager manager;

  public Event(ConnectionManager manager) {
    this.manager = manager;
  }

  /**
   * Process the event.
   * @return Returns false if the event should be handled again later.
   */
  public abstract boolean process();
}
