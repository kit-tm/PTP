package edu.kit.tm.ptp.connection;

import edu.kit.tm.ptp.Identifier;
import edu.kit.tm.ptp.crypt.CryptHelper;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.spec.InvalidKeySpecException;
import java.util.logging.Level;

/**
 * Class for the event of setting a file containing the private key.
 */

public class EventSetIdentity extends Event {
  private File privateKey;
  private Identifier identifier;

  public EventSetIdentity(ConnectionManager manager, File privateKey, Identifier identifier) {
    super(manager);

    this.privateKey = privateKey;
    this.identifier = identifier;
  }

  @Override
  public boolean process() {
    if (identifier == null || !identifier.isValid()) {
      manager.logger.log(Level.WARNING, "Identifier is invalid.");
      return true;
    }

    if (privateKey == null || !privateKey.exists()) {
      manager.logger.log(Level.WARNING, "PrivateKey is invalid.");
      return true;
    }

    try {
      manager.cryptHelper.setKeyPair(CryptHelper.readKeyPairFromFile(privateKey));
    } catch (InvalidKeyException | InvalidKeySpecException | IOException e) {
      manager.logger.log(Level.SEVERE, "Failed to set private key file.");
      return true;
    }

    manager.localIdentifier = identifier;
    manager.logger.log(Level.INFO, "Set local identifier to " + identifier);

    return true;
  }
}
