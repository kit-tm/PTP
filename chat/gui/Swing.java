package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTextPane;


public class Swing {

	private final JFrame frame = new JFrame();



	public Swing() {
		frame.setLayout(new BorderLayout());
		frame.setTitle("TorP2P Messenger");

		try {
			frame.setIconImage(ImageIO.read(new File("image/toricon.png")));
		} catch (IOException e) {

		}


		JTextPane chat = new JTextPane();
		chat.setEditable(false);
		JScrollPane chatPane = new JScrollPane(chat);
		chatPane.setPreferredSize(new Dimension(640, 480));
		frame.add(chatPane, BorderLayout.CENTER);

		JTextField input = new JTextField("Enter message...");
		frame.add(input, BorderLayout.PAGE_END);

		Vector<String> contacts = new Vector<String>();
		Vector<String> header = new Vector<String>();
		header.add("Contacts");
		JTable table = new JTable(contacts, header);
		table.setEnabled(false);
		table.setFillsViewportHeight(true);
		JScrollPane tablePane = new JScrollPane(table);
		tablePane.setPreferredSize(new Dimension(120, 480));
		frame.add(tablePane, BorderLayout.LINE_END);

		//frame.setExtendedState(Frame.MAXIMIZED_BOTH);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.pack();
		frame.setVisible(true);
	}


}
