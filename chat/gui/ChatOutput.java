package gui;

import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;


public class ChatOutput {

	private final JTextPane chat;
	private final JScrollPane chatPane;
	private final StyledDocument text;


	public ChatOutput(Dimension panelSize, Font font) {
		chat = new JTextPane();
		chat.setEditable(false);
		chat.setFont(font);
		text = chat.getStyledDocument();
		chatPane = new JScrollPane(chat);
		chatPane.setPreferredSize(panelSize);
	}


	public JScrollPane getPane() { return chatPane; }


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
