package examples;

import java.io.IOException;
import java.nio.file.Paths;

import p2p.Configuration;
import p2p.Constants;


/**
 * Outputs the parsed configuration file.
 *
 * @author Simeon Andreev
 *
 */
public class ConfigurationExample {


	public static void main(String[] args) throws IllegalArgumentException, IOException {
		Configuration configuration = new Configuration(Constants.configfile, Paths.get("config"), 9051, 9050);
		System.out.println(configuration.toString());
	}

}
