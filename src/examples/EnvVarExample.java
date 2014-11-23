package examples;

import p2p.Constants;


/**
 * Tests the TorP2P home environment variable.
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
