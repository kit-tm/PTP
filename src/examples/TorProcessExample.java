package examples;

import java.io.IOException;

import thread.TorManager;


/**
 * Test for the APIs personal Tor process execution.
 *
 * @author Simeon Andreev
 *
 */
public class TorProcessExample {


	public static void main(String[] args) throws InterruptedException, IllegalArgumentException, IOException {
		System.out.println("Creating manager.");
		TorManager tor = new TorManager();
		System.out.println("Starting Tor.");
		tor.start();
		System.out.println("Sleeping.");
		Thread.sleep(10 * 1000);
		System.out.println("Stopping Tor.");
		tor.stop();
		System.out.println("Done.");
	}

}
