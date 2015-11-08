package edu.kit.tm.ptp.raw;

import edu.kit.tm.ptp.Identifier;

import java.io.IOException;



/**
 * A listener that will be notified of connections with expired TTL. Used by the API wrapper to
 * automatically disconnect connections.
 *
 * @author Simeon Andreev
 *
 */
public interface ExpireListener {

  /**
   * Notifies this listener of a connections expired TTL.
   *
   * @param identifier The hidden service identifier whos connections TTL expired.
   * @throws IOException Propagates any IOException the API received while disconnecting a hidden
   *         service identifier.
   */
  public void expired(Identifier identifier) throws IOException;

}
