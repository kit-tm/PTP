package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.MatteBorder;


public class MessageBar {


	public static interface Listener {

		public void entered(String message);

	}


	private final JPanel panel;
	private final JTextField input;
	private final JLabel label;
	private String address = "";


	public MessageBar(Dimension componentSize, Font font, final Listener listener) {
		panel = new JPanel();
		panel.setLayout(new BorderLayout());

		input = new JTextField("");
		input.setPreferredSize(componentSize);
		input.setFont(font);
		input.addKeyListener(new KeyAdapter() {

			@Override
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() != KeyEvent.VK_ENTER) return;

				listener.entered(input.getText());
				input.setText("");
			}

		});

		label = new JLabel("Current address:");

		panel.add(input, BorderLayout.CENTER);
		panel.add(label, BorderLayout.PAGE_END);

		panel.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				StringSelection selection = new StringSelection(address);
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				clipboard.setContents(selection, selection);
			}

		});

		panel.setBorder(new MatteBorder(1, 1, 1, 1, Color.GRAY));

		disable();
	}


	public JPanel getPanel() { return panel; }


	public void setAddress(String address) { this.address = address; }

	public void setText(String text) { label.setText(text); }

	public void enable() { input.setEnabled(true); }

	public void disable() { input.setEnabled(false); }

}
