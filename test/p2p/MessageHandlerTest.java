/**
 *
 */
package p2p;

import static org.junit.Assert.*;

import java.util.Vector;

import org.junit.Before;
import org.junit.Test;

import api.Message;
import api.MessageHandler;
import api.Packet;
import utility.RNG;

/**
 * This class offers JUnit testing for the MessageAssembler class.
 *
 * @author Simeon Andreev
 *
 */
public class MessageHandlerTest {

	// TODO: rewrite after all changes are done

	/** Minimum message length to use when generating random strings. */
	private static final int minMessageLength = 10;
	/** Maximum message length to use when generating random strings. */
	private static final int maxMessageLength = 20;
	/** The number of messages to use for the test. */
	private static final int m = 50;

	/** The RNG to use when generating random strings. */
	private RNG random;


	/**
	 * @see JUnit
	 */
	@Before
	public void setUp() {
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
		for (int i = 0; i < m; ++i)
			messages[i] = random.string(minMessageLength, maxMessageLength);

		// Glue the messages into random bulks.
		final Vector<StringBuilder> bulks = new Vector<StringBuilder>();
		bulks.add(new StringBuilder(""));
		for (int i = 0; i < m; ++i)
		{
			bulks.lastElement().append(MessageHandler.wrapMessage(new Message(messages[i], null)).content);
			if (random.floating() > 0.5) bulks.add(new StringBuilder(""));
		}

		Vector<Message> received = new Vector<Message>(m);

		// Send the fragments.
		for (int i = 0; i < bulks.size(); ++i) {
			Packet[] packets = MessageHandler.unwrapBulk(bulks.get(i).toString());
			for (Packet packet : packets)
				received.add(packet.message);
		}

		for (int i = 0; i < messages.length; ++i) {
			if (!received.get(i).content.equals(messages[i]))
				fail("Received message " + i + " [" + received.get(i).content + "] does not match sent message [" + messages[i] + "].");
		}
	}

}
