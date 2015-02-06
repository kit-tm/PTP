package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;


public class Swing {

	private final static String font = "Arial";

	private final HashMap<String, String> map = new HashMap<String, String>();
	private final JFrame frame;
	private final StyledDocument text;
	private final JTextField input;
	private final DefaultTableModel model;
	private final JTable table;

	public int selected = -1;


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
		chat.setFont(new Font(font, 0, 14));
		text = chat.getStyledDocument();
		JScrollPane chatPane = new JScrollPane(chat);
		chatPane.setPreferredSize(new Dimension(640, 480));
		frame.add(chatPane, BorderLayout.CENTER);

		input = new JTextField("");
		input.setPreferredSize(new Dimension(640, 35));
		input.setFont(new Font(font, 0, 18));
		frame.add(input, BorderLayout.PAGE_END);
		input.addKeyListener(new KeyAdapter() {

			@Override
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					String message = input.getText();
					input.setText("");

					try {
						text.insertString(text.getLength(), "Sent message: " + message + "\n", null);
					} catch (BadLocationException e1) { /* useless exception n13463 ? */ }
				}
			}

		});

		model = new DefaultTableModel(new String[]{ "" }, 0) {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isCellEditable(int row, int column) { return false; };

		};

		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.setPreferredSize(new Dimension(120, 480));

		table = new JTable();
		table.setEnabled(true);
		table.setFillsViewportHeight(true);
		table.setModel(model);
		table.setFocusable(false);
		table.setRowSelectionAllowed(true);
		table.setFont(new Font(font, 0, 16));
		table.setRowHeight(20);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane tablePane = new JScrollPane(table);
		tablePane.setPreferredSize(new Dimension(120, 450));
		table.setTableHeader(null);
		final ListSelectionListener listener = new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (selected == table.getSelectedRow()) return;
				selected = table.getSelectedRow();
				if (selected == -1) return;

				try {
					text.insertString(text.getLength(), "Selected: " + table.getValueAt(selected, 0) + " (" + map.get(table.getValueAt(selected, 0)) + ")" + "\n", null);
				} catch (BadLocationException e1) { /* useless exception n13463 ? */ }
			}
		};
		table.getSelectionModel().addListSelectionListener(listener);

		final JButton button = new JButton("Add contact");
		button.setPreferredSize(new Dimension(120, 30));
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				final JTextField nickname = new JTextField();
				nickname.setPreferredSize(new Dimension(120, 25));
				nickname.addAncestorListener( new AncestorListener() {

					@Override
					public void ancestorAdded(AncestorEvent event) { nickname.requestFocusInWindow(); }

					@Override
					public void ancestorRemoved(AncestorEvent event) { }

					@Override
					public void ancestorMoved(AncestorEvent event) { }

				});

				final JTextField address = new JTextField();
				address.setPreferredSize(new Dimension(120, 25));
				final JPanel panel = new JPanel();
				panel.setLayout(new GridBagLayout());
				panel.setPreferredSize(new Dimension(160, 70));
				GridBagConstraints c = new GridBagConstraints();
				c.gridx = 0; c.gridy = 0; c.anchor = GridBagConstraints.WEST; c.ipadx = 10;
				panel.add(new JLabel("Nickname"), c);
				c.gridx = 1; c.gridy = 0; c.anchor = GridBagConstraints.EAST;
				panel.add(nickname, c);
				c.gridx = 0; c.gridy = 1; c.anchor = GridBagConstraints.WEST; c.ipadx = 10;
				panel.add(new JLabel("Address"), c);
				c.gridx = 1; c.gridy = 1; c.anchor = GridBagConstraints.EAST;
				panel.add(address, c);

				final int result = JOptionPane.showConfirmDialog(frame, panel, "Enter contact info", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null);

				if (result == JOptionPane.OK_OPTION && !nickname.getText().equals("") && !address.getText().equals("")) {
					boolean updated = false;

					if (map.containsKey(nickname.getText())) {
						for (int r = 0; r < model.getRowCount(); ++r) {
							if (model.getValueAt(r, 0).equals(nickname.getText())) {
								final int s = selected;
								model.removeRow(r);
								System.out.println("Removing " + r + ", selected is " + s);
								if (s == r) {
									selected = model.getRowCount();
									updated = true;
								}
								break;
							}
						}
					}

					map.put(nickname.getText(), address.getText());
					model.addRow(new String[]{ nickname.getText() });
					if (updated) {
						System.out.println("here");
						table.editCellAt(model.getRowCount() - 1, 0);
						// TODO: fix this
					}
				}
			}

		});

		panel.add(button, BorderLayout.PAGE_START);
		panel.add(tablePane, BorderLayout.PAGE_END);

		frame.add(panel, BorderLayout.LINE_END);

		//frame.setExtendedState(Frame.MAXIMIZED_BOTH);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.pack();
		frame.setVisible(true);
	}


}
