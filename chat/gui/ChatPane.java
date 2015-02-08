package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.border.MatteBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;


public class ChatPane {

	private final JPanel panel;
	private final JTextPane chat;
	private final JScrollPane chatPane;
	private final StyledDocument text;


	public ChatPane(Dimension panelSize, Font font) {
		panel = new JPanel();
		panel.setLayout(new BorderLayout());
		chat = new JTextPane();
		chat.setEditable(false);
		chat.setFont(font);
		text = chat.getStyledDocument();
		chatPane = new JScrollPane(chat);
		chatPane.setPreferredSize(panelSize);
		panel.add(chatPane, BorderLayout.CENTER);
		panel.setBorder(new MatteBorder(1, 1, 1, 1, Color.GRAY));
	}


	public JPanel getPanel() { return panel; }


	public void append(String line) {
		try {
			text.insertString(text.getLength(), line + "\n", null);
		} catch (BadLocationException e1) { }
	}

	public void clear() {
		try {
			text.remove(0, text.getLength());
		} catch (BadLocationException e) { }
	}


}
