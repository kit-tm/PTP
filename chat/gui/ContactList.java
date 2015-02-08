package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;


public class ContactList {


	public static interface Listener {

		public void selected(String nickname, String address);

		public void added(String nickname, String address);

		public void updated(String newNickname, String newAddress, String oldNickname, String oldAddress);

		public void deleted(String nickname, String address);

	}


	private final JFrame parent;
	private final DefaultTableModel model;
	private final JTable table;
	private final JScrollPane scrollPane;
	private final JButton button;
	private final JPanel panel;

	private final Listener listener;

	private final HashMap<String, String> map = new HashMap<String, String>();
	private int row = -1;
	private int selected = -1;


	public ContactList(final JFrame parent, Dimension panelSize, Dimension buttonSize, final Dimension promptPanelSize, final Dimension promptInputSize, final int labelPadding, Font entryFont, int entryHeight, final Listener listener) {
		this.parent = parent;

		model = new DefaultTableModel(new String[]{ "" }, 0) {

			private static final long serialVersionUID = -7364723206370459230L;


			@Override
			public boolean isCellEditable(int row, int column) { return false; };

		};

		table = new JTable();
		scrollPane = new JScrollPane(table);

		table.setEnabled(true);
		table.setFillsViewportHeight(true);
		table.setModel(model);
		table.setFocusable(false);
		table.setRowSelectionAllowed(true);
		table.setFont(entryFont);
		table.setRowHeight(entryHeight);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setTableHeader(null);
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (selected == table.getSelectedRow()) return;
				selected = table.getSelectedRow();
				if (selected == -1) return;

				final String nickname = model.getValueAt(selected, 0).toString();
				final String address = map.get(nickname);
				listener.selected(nickname, address);
			}

		});
		final JPopupMenu popupMenu = new JPopupMenu();
		JMenuItem editContact = new JMenuItem("Edit");
		editContact.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (row == -1) return;
				final int index = row;
				final String nickname = model.getValueAt(row, 0).toString();
				final String address = map.get(nickname);
				row = -1;

				final EditPrompt prompt = new EditPrompt(promptPanelSize, promptInputSize, labelPadding, nickname, address);
				final int result = JOptionPane.showConfirmDialog(parent, prompt.getPanel(), "Contact info", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null);

				if (result != JOptionPane.OK_OPTION || prompt.getNickname().equals("") || prompt.getAddress().equals("")) return;

				if (!nickname.equals(prompt.getNickname()) && map.containsKey(prompt.getNickname())) {
					JOptionPane.showMessageDialog(parent, "A contact with the same nickname already exists.");
					return;
				}

				if (!address.equals(prompt.getAddress()) && map.containsValue(prompt.getAddress())) {
					JOptionPane.showMessageDialog(parent, "A contact with the same address exists.");
					return;
				}

				map.remove(nickname);
				map.put(prompt.getNickname(), prompt.getAddress());
				model.setValueAt(prompt.getNickname(), index, 0);

				listener.updated(prompt.getNickname(), prompt.getAddress(), nickname, address);
			}

		});
		popupMenu.add(editContact);
		JMenuItem deleteContact = new JMenuItem("Delete");
		deleteContact.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (row == -1) return;
				final String nickname = model.getValueAt(row, 0).toString();
				final String address = map.get(nickname);

				model.removeRow(row);
				if (selected == row) selected = -1;
				else if (selected != -1 && selected > row)
					--selected;

				row = -1;
				map.remove(nickname);
				listener.deleted(nickname, address);
			}

		});
		popupMenu.add(deleteContact);
		table.addMouseListener(new MouseAdapter() {

			public void mouseReleased(MouseEvent e) {
				if (!e.isPopupTrigger() || table.columnAtPoint(e.getPoint()) != 0) return;
				row = table.rowAtPoint(e.getPoint());

				popupMenu.show(e.getComponent(), e.getX(), e.getY());
			}

		});

		button = new JButton("Add contact");
		button.setPreferredSize(buttonSize);
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				final EditPrompt prompt = new EditPrompt(promptPanelSize, promptInputSize, labelPadding);
				final int result = JOptionPane.showConfirmDialog(parent, prompt.getPanel(), "Contact info", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null);

				if (result != JOptionPane.OK_OPTION || prompt.getNickname().equals("") || prompt.getAddress().equals("")) return;

				add(prompt.getNickname(), prompt.getAddress());
			}

		});

		panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.setPreferredSize(panelSize);

		panel.add(button, BorderLayout.PAGE_START);
		panel.add(scrollPane, BorderLayout.CENTER);

		this.listener = listener;

		button.setEnabled(false);
		table.setEnabled(false);
	}


	public JPanel getPanel() { return panel; }


	public void add(String nickname, String address) {
		if (map.containsValue(address)) {
			JOptionPane.showMessageDialog(parent, "A contact with the same address exists.");
			return;
		}

		if (map.containsKey(nickname)) {
			JOptionPane.showMessageDialog(parent, "Contacts address updated.");
			final String oldAddress = map.get(nickname);
			listener.updated(nickname, address, nickname, oldAddress);
			return;
		}

		map.put(nickname, address);
		model.addRow(new String[]{ nickname });
		listener.added(nickname, address);
	}

	public String getAddress(String nickname) { return map.get(nickname); }

	public boolean hasContact(String address) { return map.containsValue(address); }

	public void enable() {
		button.setEnabled(true);
		table.setEnabled(true);
	}

}
