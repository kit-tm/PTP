package gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JTextField;


public class MessageInput {


	public static interface Listener {

		public void entered(String message);

	}


	private final JTextField input;


	public MessageInput(Dimension componentSize, Font font, final Listener listener) {
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
		disable();
	}


	public Component getComponent() { return input; }


	public void enable() { input.setEnabled(true); }

	public void disable() { input.setEnabled(false); }

}
