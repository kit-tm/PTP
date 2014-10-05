package test;

import thread.TorManager;


/**
 * Tests the APIs personal Tor process execution.
 *
 * @author Simeon Andreev
 *
 */
public class TorProcessTest {


	public static void main(String[] args) throws InterruptedException {
		System.out.println("Creating manager.");
		TorManager tor = new TorManager();
		System.out.println("Starting Tor.");
		tor.start();
		System.out.println("Sleeping.");
		Thread.sleep(15 * 1000);
		System.out.println("Stopping Tor.");
		tor.stop();
		System.out.println("Done.");
	}

}
