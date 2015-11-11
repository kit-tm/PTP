package edu.kit.tm.ptp.examples;

import edu.kit.tm.ptp.utility.Constants;


/**
 * Example access of the PTP home environment variable.
 *
 * @author Simeon Andreev
 *
 */
public class EnvVarExample {

  public static void main(String[] args) {
    String ptphome = System.getenv(Constants.ptphome);
    System.out.println(Constants.ptphome + " is set to " + ptphome);
  }

}
