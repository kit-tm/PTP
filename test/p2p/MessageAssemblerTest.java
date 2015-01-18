/**
 *
 */
package p2p;

import static org.junit.Assert.*;

import java.util.Vector;

import org.junit.Before;
import org.junit.Test;

import callback.ReceiveListener;
import api.MessageHandler;
import utility.RNG;

/**
 * This class offers JUnit testing for the MessageAssembler class.
 *
 * @author Simeon Andreev
 *
 */
public class MessageAssemblerTest {

	/** Minimum message length to use when generating random strings. */
	private static final int minMessageLength = 10;
	/** Maximum message length to use when generating random strings. */
	private static final int maxMessageLength = 20;
	/** Minimum fragment length to use when cutting original messages. */
	private static final int minFragmentLength = 5;
	/** Maximum fragment length to use when cutting original messages. */
	private static final int maxFragmentLength = 30;
	/** The number of messages to use for the test. */
	private static final int m = 50;

	/** The message assembler to use for the tests. */
	private MessageHandler messageWrapper;
	/** The RNG to use when generating random strings. */
	private RNG random;


	/**
	 * @see JUnit
	 */
	@Before
	public void setUp() {
		messageWrapper = new MessageHandler();
		random = new RNG();
	}



	/**
	 * Generates random messages and cuts them into random fragments, checks if the original messages are property assembled.
	 *
	 * Fails iff a message is not properly assembled.
	 */
	@Test
	public void test() {
		// Generate random strings.
		final String[] messages = new String[m];
		for (int i = 0; i < messages.length; ++i)
			messages[i] = random.string(minMessageLength, maxMessageLength);

		// Add the strings to a string buffer.
		final StringBuilder builder = new StringBuilder("");
		for (int i = 0; i < messages.length; ++i)
			builder.append(messageWrapper.alterContent(messages[i]));

		// Set the receiving listener.
		final Vector<String> received = new Vector<String>();
		messageWrapper.setListener(new ReceiveListener() {

			@Override
			public void receivedMessage(byte[] message) { received.add(new String(message)); }

		});

		// Cut the messages into random fragments.
		final Vector<String> fragments = new Vector<String>();
		while (builder.length() > 0) {
			final int length = Math.min(builder.length(), random.integer(minFragmentLength, maxFragmentLength));
			final String content = builder.substring(0, length);
			builder.replace(0, length, "");
			fragments.add(content);
		}

		// Send the fragments.
		for (String fragment : fragments)
			messageWrapper.receivedMessage(fragment.getBytes());

		for (int i = 0; i < messages.length; ++i) {
			if (!received.get(i).equals(messages[i]))
				fail("Received message " + i + " [" + received.get(i) + "] does not match sent message [" + messages[i] + "].");
		}
	}

}
