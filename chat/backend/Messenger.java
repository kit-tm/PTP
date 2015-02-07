package backend;

import java.io.IOException;
import java.util.HashMap;

import adapters.ReceiveListenerAdapter;
import adapters.SendListenerAdapter;
import api.Identifier;
import api.Message;
import api.TorP2P;


public class Messenger {


	public static interface Listener {


		public void received(String message, String origin);

		public void sent(long id);

		public void failed(long id);

	}


	private final ReceiveListenerAdapter receiveListener = new ReceiveListenerAdapter() {

		@Override
		public void receivedMessage(Message message) {
			final String address = message.identifier.getTorAddress();
			log(message.content, address);
			listener.received(message.content, address);
		}

	};

	private final SendListenerAdapter sendListener = new SendListenerAdapter() {

		@Override
		public void connectionTimeout(Message message) { listener.failed(message.id); }


		@Override
		public void sendSuccess(Message message) { listener.sent(message.id); }

		@Override
		public void sendFail(Message message) { listener.failed(message.id); }

	};

	private final HashMap<String, Chatroom> rooms = new HashMap<String, Chatroom>();
	private final Listener listener;
	private final TorP2P client;
	private final Identifier identifier;
	private long id = 0;



	public Messenger(Listener listener) throws IllegalArgumentException, IOException {
		this.listener = listener;
		client =  new TorP2P();
		client.SetListener(receiveListener);
		identifier = client.GetIdentifier();
	}


	public long sendMessage(String message, String destination, String nickname) {
		++id;
		client.SendMessage(new Message(id, message, new Identifier(destination)), 60 * 1000, sendListener);
		log(message, identifier.getTorAddress());
		return id;
	}

	public void relocateChatroom(String from, String to) {
		if (!rooms.containsKey(from)) return;

		rooms.put(to, rooms.get(from));
		rooms.remove(from);
	}

	public String getAddress() {
		return identifier.getTorAddress();
	}

	public void cleanUp() {
		client.Exit();
	}

	private void log(String message, String address) {
		if (!rooms.containsKey(address)) rooms.put(address, new Chatroom());
		rooms.get(address).addMessage(address, message);
	}

}
