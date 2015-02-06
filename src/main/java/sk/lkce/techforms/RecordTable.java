package sk.lkce.techforms;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

public class RecordTable {

	private JDialog dialog;
	private JTable table;
	private JButton button;

	public RecordTable(JFrame frame, final List<Record> records,
			final GenerateForRecordsCallback callback) {
		dialog = new JDialog(frame);
		dialog.setTitle("Parsed records");
		RecordTableModel model = new RecordTableModel(records);
		table = new JTable(model);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		JScrollPane scrollPane = new JScrollPane(table);

		dialog.add(scrollPane);
		dialog.setSize(1200, 600);
		dialog.setLocationRelativeTo(null);
		button = new JButton("Generate from selected");

		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				int[] rows = table.getSelectedRows();

				List<Record> selected = new ArrayList<>();
				for (int index : rows)
					selected.add(records.get(index));

				callback.generateForRecords(selected);
				dialog.dispose();
			}
		});

		JPanel bottomPanel = new JPanel();
		bottomPanel.add(button);

		dialog.add(bottomPanel, BorderLayout.SOUTH);

	}

	public void showDialog() {
		for (int col = 0; col < table.getColumnCount(); col++) {
			int width = 100;
			for (int row = 0; row < table.getRowCount(); row++) {
				TableCellRenderer renderer = table.getCellRenderer(row, col);
				Component comp = table.prepareRenderer(renderer, row, col);
				width = Math.max(comp.getPreferredSize().width, width);
			}
			System.out.println(width);
			if (width > 300)
				width = 300;
			table.getColumnModel().getColumn(col).setPreferredWidth(width);
		}
		dialog.setVisible(true);

	}

}
