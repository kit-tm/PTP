package gui;

import java.awt.Color;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.border.MatteBorder;


public class InfoWindow {

	private final JWindow window;
	private final JLabel label;


	public InfoWindow(JFrame frame) {
		window = new JWindow();
		label = new JLabel("", SwingConstants.CENTER);
		window.getContentPane().add(label);
		window.setBounds(0, 0, 100, 30);
		window.setLocationRelativeTo(frame);
		window.getRootPane().setBorder(new MatteBorder(2, 2, 2, 2, Color.BLACK));
	}


	public void setMessage(String message) { label.setText(message); }

	public void show() { window.setVisible(true); }

	public void hide() { window.setVisible(false); }

	public void destroy() { window.dispose(); }

}
