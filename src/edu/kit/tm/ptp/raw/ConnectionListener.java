package edu.kit.tm.ptp.raw;

import edu.kit.tm.ptp.Identifier;

import java.net.Socket;


/**
 * A listener interface to notify whenever a connection to the local Hidden Service is made.
 *
 * @author Simeon Andreev
 *
 */
public interface ConnectionListener {


  public void ConnectionOpen(Identifier identifier, Socket socket);

}
