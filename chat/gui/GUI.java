package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import backend.Chatroom;
import backend.Messenger;


public class GUI {




	private final JFrame frame;

	private final InfoWindow info;
	private final Messenger messenger;
	private final ContactList contacts;
	private final ChatOutput chat;
	private final MessageInput input;

	public String current = null;


	public GUI() throws IllegalArgumentException, IOException {
		frame = new JFrame();
		frame.setLayout(new BorderLayout());

		frame.setIconImage(ImageIO.read(new File("image/toricon.png")));

		chat = new ChatOutput(new Dimension(640, 480), new Font("Arial", 0, 14));

		input = new MessageInput(new Dimension(640, 35), new Font("Arial", 0, 18), new MessageInput.Listener() {

			@Override
			public void entered(String message) {
				// TODO: mark the message as not yet sent in the log
				// TODO: assign the message an id
				messenger.sendMessage(message, contacts.getAddress(current));
				chat.append("Me: " + message);
			}

		});

		contacts = new ContactList(frame, new Dimension(120, 480), new Dimension(120, 30), new Dimension(160, 70), new Dimension(120, 25), 10, new Font("Arial", 0, 16), 20, new ContactList.Listener() {

			@Override
			public void selected(String nickname, String address) {
				current = nickname;
				input.enable();
				showMessages();
			}

			@Override
			public void updated(String newNickname, String newAddress, String oldNickname, String oldAddress) {
				messenger.relocateChatroom(oldAddress, newAddress);
				if (current.equals(oldNickname)) {
					current = newNickname;
					showMessages();
				}
			}

			@Override
			public void added(String nickname, String address) {
				messenger.addChatroom(address);
			}

			@Override
			public void deleted(String nickname, String address) {
				if (nickname.equals(current)) {
					input.disable();
					chat.clear();
				}
			}

		});

		frame.add(input.getComponent(), BorderLayout.PAGE_END);
		frame.add(chat.getPane(), BorderLayout.CENTER);
		frame.add(contacts.getPanel(), BorderLayout.LINE_END);

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				Thread thread = new Thread() {

					@Override
					public void run() {
						info.setMessage("Stopping Tor...");
						messenger.cleanUp();
						info.hide();
						info.destroy();
					}

				};
				thread.start();
			}

		});

		frame.pack();
		frame.setVisible(true);

		info = new InfoWindow(frame);
		info.setMessage("Bootstrapping Tor...");
		info.show();

		messenger = new Messenger(new Messenger.Listener() {

			@Override
			public void sent(long id) {
				// TODO: mark the message as sent in the chat

			}

			@Override
			public void received(String message, String origin) {
				if (contacts.hasContact(origin)) {
					if (contacts.getAddress(current).equals(origin))
						chat.append(current + ": " + message);
					// TODO: else indicate that a message was received by the contact
				} else {
					contacts.add(origin, origin);
				}
			}

			@Override
			public void failed(long id) {
				// TODO: mark message sending fail
			}

		});

		info.hide();
		frame.setTitle("Current address: " + messenger.getAddress());
		contacts.enable();
	}


	private void showMessages() {
		chat.clear();
		final Chatroom.Message[] messages = messenger.getMessages(contacts.getAddress(current));
		for (int i = 0; i < messages.length; ++i)
			chat.append((messages[i].remote ? current : "Me") + ": " + messages[i].content);
	}

}
