package backend;

import java.util.Date;
import java.util.Vector;


public class Chatroom {


	public static class Message {

		public final String content;
		public final Date timestamp;
		public final boolean remote;


		public Message(String content, Date timestamp, boolean remote) {
			this.content = content;
			this.timestamp = timestamp;
			this.remote = remote;
		}

	}


	private final Vector<Message> unread = new Vector<Message>();


	public Chatroom() { }


	public void addMessage(String content, boolean remote) {
		unread.add(new Message(content, new Date(), remote));
	}

	public Message[] getMessages() {
		Message[] messages = new Message[unread.size()];

		for (int i = 0; i < unread.size(); ++i)
			messages[i] = unread.get(i);

		return messages;
	}

}
