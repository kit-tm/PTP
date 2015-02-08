package gui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;


public class EditPrompt {

	final JPanel panel;
	final JTextField nickname;
	final JTextField address;


	public EditPrompt(Dimension panelSize, Dimension inputSize, int labelPadding) {
		this(panelSize, inputSize, labelPadding, "", "");
	}

	public EditPrompt(Dimension panelSize, Dimension inputSize, int labelPadding, String nick, String addr) {
		nickname = new JTextField(nick);
		nickname.setPreferredSize(inputSize);
		nickname.addAncestorListener( new AncestorListener() {

			@Override
			public void ancestorAdded(AncestorEvent event) { nickname.requestFocusInWindow(); }

			@Override
			public void ancestorRemoved(AncestorEvent event) { }

			@Override
			public void ancestorMoved(AncestorEvent event) { }

		});

		address = new JTextField(addr);
		address.setPreferredSize(inputSize);
		panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		panel.setPreferredSize(panelSize);
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0; c.gridy = 0; c.anchor = GridBagConstraints.WEST; c.ipadx = labelPadding;
		panel.add(new JLabel("Nickname"), c);
		c.gridx = 1; c.gridy = 0; c.anchor = GridBagConstraints.EAST;
		panel.add(nickname, c);
		c.gridx = 0; c.gridy = 1; c.anchor = GridBagConstraints.WEST; c.ipadx = labelPadding;
		panel.add(new JLabel("Address"), c);
		c.gridx = 1; c.gridy = 1; c.anchor = GridBagConstraints.EAST;
		panel.add(address, c);
	}


	public JPanel getPanel() { return panel; }


	public String getNickname() { return nickname.getText(); }

	public String getAddress() { return address.getText(); }

}
