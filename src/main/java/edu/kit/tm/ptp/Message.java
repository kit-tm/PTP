package edu.kit.tm.ptp;


/**
 * A wrapper for API messages.
 *
 * @author Simeon Andreev
 *
 */
public class Message {

  /** The content of the message. */
  public final String content;
  /** The Tor hidden service destination identifier. */
  public final Identifier identifier;
  
  public Message() {
    this.content = null;
    this.identifier = null;
  }


  /**
   * Constructor method.
   *
   * @param content The content of the message.
   * @param identifier The Tor hidden service destination identifier.
   */
  public Message(String content, Identifier identifier) {
    this.content = content;
    this.identifier = identifier;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof Message)) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    Message msg = (Message) obj;
    return content.equals(msg.content) && identifier.equals(msg.identifier);
  }

}
