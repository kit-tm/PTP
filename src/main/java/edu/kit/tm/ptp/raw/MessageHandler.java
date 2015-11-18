package edu.kit.tm.ptp.raw;

import edu.kit.tm.ptp.Message;
import edu.kit.tm.ptp.utility.Constants;

import java.util.Vector;


/**
 * This class offers static methods for handling messages received via the API. Messages are assumed
 * to be at most glued during unwrapping, i.e. bulk messages are supported, fragmentation is not.
 *
 *
 * @author Simeon Andreev
 *
 */
public class MessageHandler {


  /**
   * Removes the meta information from a packet bulk and extracts the messages in the bulk.
   *
   * @param bulk The packet bulk.
   *
   * @return The packets in the bulk.
   */
  public static Packet[] unwrapBulk(String bulk) {
    Vector<Packet> buffer = new Vector<Packet>();
    int index = 0;

    // Check if we can complete the current message and possibly further messages.
    while (true) {
      // Get the position of the next wrapped message length delimiter.
      final int position = bulk.indexOf(Constants.messagelengthdelimiter, index);
      // If no length delimiter is found we are done.
      if (position == -1) {
        break;
      }
      // Fetch the message length.
      final int length = Integer.valueOf(bulk.substring(index, position));
      // Fetch the message flag.
      final char flags = bulk.charAt(position + 1);
      // Fetch the message.
      final String content = bulk.substring(position + 2, position + 2 + length);
      // Add the message to the message buffer.
      buffer.add(new Packet(new Message(content, null), flags));
      // Move to the next message.
      index = position + 2 + length;
    }

    Packet[] packets = new Packet[buffer.size()];
    for (int i = 0; i < buffer.size(); ++i) {
      packets[i] = buffer.get(i);
    }

    return packets;
  }


  /**
   * Wraps a message into a message with meta information.
   *
   * @param message The message which should be wrapped.
   * @return The wrapped message.
   */
  public static Message wrapMessage(Message message) {
    return new Message(wrapRaw(message.content, Constants.messagestandardflag), message.identifier);
  }


  /**
   * Wrap a raw string in the format length|delimiter|flags|message, where length is the message
   * length.
   *
   * @param content The String to wrap.
   * @param flags The flags to add to the message.
   * @return The wrapped message.
   */
  public static String wrapRaw(String content, char flags) {
    return content.length() + "" + Constants.messagelengthdelimiter + "" + flags + "" + content;
  }

}
