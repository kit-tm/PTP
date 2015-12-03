package edu.kit.tm.ptp;

/**
 * Interface.
 *
 * @author Timon Hackenjos
 */
public interface MessageReceivedListener <T> {
    void messageReceived(T message, Identifier source);
}
