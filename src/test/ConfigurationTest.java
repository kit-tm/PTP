package test;

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
public class ConfigurationTest {


	public static void main(String[] args) throws IllegalArgumentException, IOException {
		Configuration configuration = new Configuration(Paths.get("config"), Constants.configfile);
		System.out.println(configuration.toString());
	}

}
