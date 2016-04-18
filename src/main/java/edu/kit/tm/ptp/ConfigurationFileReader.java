package edu.kit.tm.ptp;

import edu.kit.tm.ptp.utility.Constants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.logging.Logger;


public class ConfigurationFileReader {
  /** The logger for this class. */
  private Logger logger;
  private File configurationFile;

  // Expose constants for testing.

  /** The delimiter used in the configuration file to separate property keys and values. */
  public static final String delimiter = " ";
  /** The Symbol used for commented lines in the configuration file. */
  public static final String comment = "#";

  /** Configuration file property names. */
  public static final String DefaultIdentifier = "DefaultIdentifier";
  public static final String HiddenServicePort = "HiddenServicePort";
  // TODO: eventually support authentication types
  // public static final String AuthenticationType = "AuthenticationType";
  public static final String TorBootstrapTimeout = "TorBootstrapTimeout";
  public static final String SocketTTL = "SocketTTL";
  public static final String SocketTTLPoll = "TTLPoll";
  public static final String LoggerConfigFile = "LoggerConfigFile";


  /**
   * Constructor method.
   *
   * @param configurationFilename The path and name of the configuration file.
   */
  public ConfigurationFileReader(String configurationFilename) {
    configurationFile = new File(configurationFilename);
  }

  /**
   * Reads the configuration from a file.
   * 
   * @throws IOException Throws an IOException if unable to read or find the input configuration or
   *         control port file.
   */
  public Configuration readFromFile() throws IOException {
    if (!configurationFile.exists()) {
      throw new FileNotFoundException(
          "Configuration file does not exist: " + configurationFile.getName());
    }

    BufferedReader buffer = new BufferedReader(
        new InputStreamReader(new FileInputStream(configurationFile), Constants.charset));
    HashMap<String, String> properties = new HashMap<String, String>();

    // Read the entries of the configuration file in the map.
    int lineCount = 0;
    while (buffer.ready()) {
      String line = buffer.readLine();
      ++lineCount;

      // Skip empty lines.
      if (line.isEmpty()) {
        continue;
      }
      // Skip commented lines.
      if (line.startsWith(comment)) {
        continue;
      }

      String[] pair = line.split(delimiter);

      // Entries must be key value pairs separated by the delimiter.
      if (pair.length != 2) {
        buffer.close();
        throw new IllegalArgumentException("Configuration file line " + lineCount
            + " must be in the form: key" + delimiter + "value");
      }

      // Add the entry.
      properties.put(pair[0], pair[1]);
    }

    buffer.close();

    String loggerConfiguration;
    // Check if the configuration file contains an entry for the logger configuration.
    if (properties.containsKey(LoggerConfigFile)) {
      loggerConfiguration = properties.get(LoggerConfigFile);
      System.setProperty(Constants.loggerconfig, loggerConfiguration);
    } else {
      loggerConfiguration = "";
    }

    // Create the logger AFTER its configuration file has been set.
    logger = Logger.getLogger(ConfigurationFileReader.class.getName());
    logger.info("Set the logger properties file to: " + loggerConfiguration);

    Configuration config = new Configuration();
    config.setLoggerConfiguration(loggerConfiguration);

    // Check if all the needed properties are in the configuration file.
    check(properties, DefaultIdentifier);
    check(properties, HiddenServicePort);
    check(properties, TorBootstrapTimeout);
    check(properties, SocketTTL);
    check(properties, SocketTTLPoll);


    // Set the configuration parameters.
    String defaultIdentifier = properties.get(DefaultIdentifier);
    config.setDefaultIdentifier(defaultIdentifier);
    logger.info("Read " + DefaultIdentifier + " = " + defaultIdentifier);

    int hiddenServicePort = parse(properties, HiddenServicePort);
    config.setHiddenServicePort(hiddenServicePort);
    logger.info("Read " + HiddenServicePort + " = " + hiddenServicePort);

    byte[] authenticationBytes = new byte[0];
    config.setAuthenticationBytes(authenticationBytes);

    int bootstrapTimeout = parse(properties, TorBootstrapTimeout);
    config.setBootstrapTimeout(bootstrapTimeout);
    logger.info("Read " + TorBootstrapTimeout + " = " + bootstrapTimeout);

    int socketTtl = parse(properties, SocketTTL);
    config.setSocketTtl(socketTtl);
    logger.info("Read " + SocketTTL + " = " + socketTtl);

    int ttlPoll = parse(properties, SocketTTLPoll);
    config.setTtlPoll(ttlPoll);
    logger.info("Read " + SocketTTLPoll + " = " + ttlPoll);

    return config;
  }

  /**
   * Check if a string-to-string hash map contains a specific key.
   *
   * @param map The hash map to be checked.
   * @param key The key to be looked for in the map.
   */
  private void check(HashMap<String, String> map, String key) {
    logger.info("Checking if the configuration file contains the " + key + " property.");
    if (!map.containsKey(key)) {
      throw new IllegalArgumentException(
          "Configuration file does not contain the " + key + " property.");
    }
  }

  /**
   * Parses the integer value of a specific key in a string-to-string hash map.
   *
   * @param map The hash map containing the key value pair.
   * @param key The key of the value to be parsed.
   */
  private int parse(HashMap<String, String> map, String key) {
    logger.info("Parsing integer value of the " + key + " property: " + map.get(key));
    int value = 0;

    try {
      value = Integer.parseInt(map.get(key));
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          "Could not parse the integer value of the " + key + " property.");
    }

    return value;

  }
}
