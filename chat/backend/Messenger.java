package backend;

import java.util.HashMap;

import api.Message;
import callback.ReceiveListener;
import callback.SendListener;


public class Messenger implements ReceiveListener, SendListener {


	private final HashMap<String, Chatroom> rooms = new HashMap<String, Chatroom>();


	@Override
	public void receivedMessage(Message message) {
		final String address = message.identifier.getTorAddress();

		if (!rooms.containsKey(address)) rooms.put(address, new Chatroom());
		rooms.get(address).addMessage(address, message.content);
	}


	@Override
	public void connectionSuccess(Message message) { }

	@Override
	public void connectionTimeout(Message message) { }


	@Override
	public void sendSuccess(Message message) {
		// TODO Auto-generated method stub

	}


	@Override
	public void sendFail(Message message) {
		// TODO Auto-generated method stub

	}

}
