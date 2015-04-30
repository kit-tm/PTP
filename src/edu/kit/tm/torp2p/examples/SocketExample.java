package edu.kit.tm.torp2p.examples;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;


/**
 * Example of using the Tor SOCKS proxy to send a message to a hidden service identifier.
 *
 * @author Simeon Andreev
 *
 */
public class SocketExample {


/*
getID() ---> return HiddenService-Identifier (also z.B. einfach X in
X.onion als String)

sendMessage(destination, message) ---> dest ist eine HiddenService-ID,
message kann am Anfang zum testen einfach nur ein String sein; wird
realisiert über eine TCP-Verbindung über SOCKS, wobei der Tor-Daemon das
SOCKS-Proxy ist

...und eine Art Callback bzw. Receive-Funktion für Nachrichten.
*/

// http://logging.apache.org/log4j/1.2/manual.html

	public static void main(String[] args) throws IOException, InterruptedException {
		Thread thread = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					ServerSocket server = new ServerSocket(8080);
					Socket client = server.accept();
					//Socket client = new Socket("127.0.0.1", 8080);
					final int message = client.getInputStream().read();
					System.out.println("Client message received (message = " + message + ").");
					client.close();
					server.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		});
		thread.start();
		System.out.println("Server thread started.");

		Thread.sleep(5 * 1000);

		try {
			InetSocketAddress midpoint = new InetSocketAddress("127.0.0.1", 9050);
			Proxy proxy = new Proxy(Proxy.Type.SOCKS, midpoint);
			Socket socket = new Socket(proxy);
			InetSocketAddress destination = new InetSocketAddress("rkjapy744g3izr4y.onion", 8080);
			//Socket socket = new Socket("127.0.0.1", 8080);
			socket.connect(destination);
			socket.getOutputStream().write(new byte[]{7});
			socket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("Client message sent.");

		thread.join();
	}

}
