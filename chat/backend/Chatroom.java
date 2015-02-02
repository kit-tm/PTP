package backend;

import java.util.Date;
import java.util.Vector;


public class Chatroom {


	public static class Message {

		public final String user;
		public final String content;
		public final Date timestamp;


		public Message(String user, String content, Date timestamp) {
			this.user = user;
			this.content = content;
			this.timestamp = timestamp;
		}

	}


	private final Vector<Message> unread = new Vector<Message>();


	public Chatroom() { }


	public void addMessage(String user, String content) {
		unread.add(new Message(user, content, new Date()));
	}

	public Message[] getMessages() {
		Message[] messages = new Message[unread.size()];

		for (int i = 0; i < unread.size(); ++i)
			messages[i] = unread.get(i);

		return messages;
	}

}
