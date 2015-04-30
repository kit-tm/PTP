package edu.kit.tm.torp2p.utility;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Date;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.NtpV3Packet;
import org.apache.commons.net.ntp.TimeInfo;


/**
 * Utility class for fetching Internet time.
 *
 * @see http://www.rgagnon.com/javadetails/java-0589.html
 *
 */
public final class DateUtils {


	public static class Time {

		public final long internet;
		public final long local;


		public Time(long internet, long local) { this.internet = internet; this.local = local; }

	}


	public static final String TIME_SERVER = "0.de.pool.ntp.org";


	public final static Time getAtomicTime() throws IOException{
		NTPUDPClient timeClient = new NTPUDPClient();
		InetAddress inetAddress = InetAddress.getByName(TIME_SERVER);
		TimeInfo timeInfo = timeClient.getTime(inetAddress);
		final long current = System.currentTimeMillis();
		NtpV3Packet message = timeInfo.getMessage();
		return new Time(message.getTransmitTimeStamp().getTime(), current);
	}


	public static void main(String args[]) throws IOException {
		System.out.println(new Date(DateUtils.getAtomicTime().internet));
	}

}