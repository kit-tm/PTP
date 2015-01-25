package callback;

import java.net.Socket;

import api.Identifier;


/**
 * A listener interface to notify whenever a connection to the local Hidden Service is made.
 *
 * @author Simeon Andreev
 *
 */
public interface ConnectionListener {


	public void ConnectionOpen(Identifier identifier, Socket socket);

}
