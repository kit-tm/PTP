package edu.kit.tm.torp2p.examples;

import java.io.IOException;
import java.net.Socket;

import edu.kit.tm.torp2p.raw.TorManager;

import net.freehaven.tor.control.TorControlConnection;


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

		long waited = 0;
		while (!tor.ready() && tor.running() && waited < 60 * 1000) {
			try {
				final long start = System.currentTimeMillis();
				Thread.sleep(250);
				waited += System.currentTimeMillis() - start;
			} catch (InterruptedException e) {
				// Waiting was interrupted. Do nothing.
			}
		}

		Socket s = new Socket("127.0.0.1", tor.controlport());
		TorControlConnection conn = new TorControlConnection(s);
		conn.authenticate(new byte[0]);
		System.out.println(conn.getInfo("net/listeners/socks"));
		System.out.println(conn.getInfo("status/bootstrap-phase"));


		System.out.println("Sleeping.");
		Thread.sleep(10 * 1000);
		System.out.println("Stopping Tor.");
		tor.stop();
		System.out.println("Done.");
	}

}
