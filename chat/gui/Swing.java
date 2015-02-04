package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTextPane;


public class Swing {

	private final JFrame frame;
	private final JScrollPane chatPane;
	private final JTextField input;
	private final JTable table;

	//private String currentAddress = "";


	public Swing() {
		frame = new JFrame();
		frame.setLayout(new BorderLayout());
		frame.setTitle("TorP2P Messenger");

		try {
			frame.setIconImage(ImageIO.read(new File("image/toricon.png")));
		} catch (IOException e) {

		}

		JTextPane chat = new JTextPane();
		chat.setEditable(false);
		chatPane = new JScrollPane(chat);
		chatPane.setPreferredSize(new Dimension(640, 480));
		frame.add(chatPane, BorderLayout.CENTER);

		input = new JTextField("");
		frame.add(input, BorderLayout.PAGE_END);
		input.addKeyListener(new KeyAdapter() {

			@Override
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					String message = input.getText();
					input.setText("");
					System.out.println("Sent message: " + message);
				}
			}

		});

		Vector<String> contacts = new Vector<String>();
		Vector<String> header = new Vector<String>();
		header.add("");
		table = new JTable(contacts, header);
		table.setEnabled(false);
		table.setFillsViewportHeight(true);
		JScrollPane tablePane = new JScrollPane(table);
		tablePane.setPreferredSize(new Dimension(120, 480));
		frame.add(tablePane, BorderLayout.LINE_END);
		final JButton button = new JButton("Add contact");
		button.setPreferredSize(new Dimension(120, 30));
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("Clicked add contact button");
			}

		});
		table.getTableHeader().setPreferredSize(new Dimension(120, 30));
		table.getTableHeader().setLayout(new FlowLayout(FlowLayout.CENTER, 10, 0));
		table.getTableHeader().add(button);

		//frame.setExtendedState(Frame.MAXIMIZED_BOTH);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.pack();
		frame.setVisible(true);
	}


}
