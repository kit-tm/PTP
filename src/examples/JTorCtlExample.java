package examples;

import java.net.Socket;
import java.util.Arrays;
import net.freehaven.tor.control.TorControlConnection;


/**
 * Uses the JTorCtl API in the context of creating hidden services.
 *
 * @author Simeon Andreev
 *
 */
public class JTorCtlExample {


	public static void main(String[] args) {
		try {
			Socket s = new Socket("127.0.0.1", 9051);
			TorControlConnection conn = TorControlConnection.getConnection(s);
			conn.authenticate(new byte[0]);

			conn.setConf(Arrays.asList(new String[]{
				"HiddenServiceDir /home/simeon/hidden-service",
				"HiddenServicePort 80 127.0.0.1:80",
				"HiddenServicePort 443 127.0.0.1:443",
			}));

	        /*// Get one configuration variable.
	        List options = conn.getConf("contact");
	        // Get a set of configuration variables.
	        List options = conn.getConf(Arrays.asList(new String[]{
	               "contact", "orport", "socksport"}));
	        // Change a single configuration variable
	        conn.setConf("BandwidthRate", "1 MB");
	        // Change several configuration variables
	        conn.setConf(Arrays.asList(new String[]{
	               "HiddenServiceDir /home/tor/service1",
	               "HiddenServicePort 80",
	        }));
	        // Reset some variables to their defaults
	        conn.resetConf(Arrays.asList(new String[]{
	               "contact", "socksport"
	        }));
	        // Flush the configuration to disk.
	        conn.saveConf();*/
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
