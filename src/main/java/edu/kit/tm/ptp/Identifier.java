package edu.kit.tm.ptp;

import java.util.regex.Pattern;

/**
 * A wrapper for Tor hidden service addresses.
 *
 * @author Simeon Andreev
 *
 */
public class Identifier {

  /** The Tor hidden service address of this identifier. */
  private String address;
  
  private static final Pattern validOnionAddress = Pattern.compile("[a-z2-7]{16}|[a-z2-7]{56}.onion");
  
  public Identifier() {
    address = null;
  }


  /**
   * Constructor method.
   *
   * @param address The Tor hidden service address of the constructed identifier.
   */
  public Identifier(String address) {
    this.address = address;
  }


  /**
   * Returns the Tor hidden service address of this identifier.
   *
   * @return The Tor hidden service address of this identifier.
   */
  public String getTorAddress() {
    return address;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof Identifier)) {
      return false;
    }
    if (this == obj) {
      return true;
    }
    return address.equals(((Identifier) obj).address);
  }

  /**
   * Returns the hash of the underlying address.
   *
   * @return The hash of the underlying address.
   *
   * @see Object
   */
  @Override
  public int hashCode() {
    return address.hashCode();
  }

  @Override
  public String toString() {
    return address;
  }
  
  public boolean isValid() {
    return validOnionAddress.matcher(address).matches();
  }

}
