package edu.kit.tm.torp2p.examples;

import java.io.IOException;

import edu.kit.tm.torp2p.raw.Configuration;
import edu.kit.tm.torp2p.utility.Constants;



/**
 * Outputs the parsed configuration file.
 *
 * @author Simeon Andreev
 *
 */
public class ConfigurationExample {


	public static void main(String[] args) throws IllegalArgumentException, IOException {
		Configuration configuration = new Configuration(Constants.configfile);
		configuration.setTorConfiguration("./config", 9051, 9050);
		System.out.println(configuration.toString());
	}

}
