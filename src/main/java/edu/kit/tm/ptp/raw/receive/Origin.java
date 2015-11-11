package edu.kit.tm.ptp.raw.receive;

import edu.kit.tm.ptp.Identifier;

import java.io.ObjectInputStream;
import java.net.Socket;


/**
 * A pair of a socket and an identifier. And an object input stream.
 *
 * @author Simeon Andreev
 *
 */
public class Origin {


  /** The socket. */
  public final Socket socket;
  /** The identifier of the socket. */
  public Identifier identifier = null;
  /** The input stream for the socket. */
  public ObjectInputStream inStream;


  /**
   * Constructor method.
   *
   * @param address The Tor hidden service identifier of the pair.
   * @param socket The socket of the pair.
   */
  public Origin(String address, Socket socket) {
    this.identifier = new Identifier(address);
    this.socket = socket;
  }
}
