package test;

import java.io.IOException;
import java.nio.file.Paths;

import p2p.Configuration;
import p2p.Constants;
import thread.TorManager;


/**
 * Tests the APIs personal Tor process execution.
 *
 * @author Simeon Andreev
 *
 */
public class TorProcessTest {


	public static void main(String[] args) throws InterruptedException, IllegalArgumentException, IOException {
		@SuppressWarnings("unused")
		Configuration c = new Configuration(Paths.get("config"), Constants.configfile);
		System.out.println("Creating manager.");
		TorManager tor = new TorManager();
		System.out.println("Starting Tor.");
		tor.start();
		System.out.println("Sleeping.");
		//Thread.sleep(30 * 1000);
		System.out.println("Stopping Tor.");
		tor.stop();
		System.out.println("Done.");
	}

}
