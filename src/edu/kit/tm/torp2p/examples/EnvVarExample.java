package edu.kit.tm.torp2p.examples;

import edu.kit.tm.torp2p.utility.Constants;


/**
 * Example access of the TorP2P home environment variable.
 *
 * @author Simeon Andreev
 *
 */
public class EnvVarExample {

	public static void main(String[] args) {
		String torp2phome = System.getenv(Constants.torp2phome);
		System.out.println(Constants.torp2phome + " is set to " + torp2phome);
	}

}
