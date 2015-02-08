package sk.lkce.techforms;

import java.util.List;

import javax.swing.table.AbstractTableModel;

@SuppressWarnings("serial")
public class RecordTableModel extends AbstractTableModel{
	
	private List<Record> records;
	
	public RecordTableModel(List<Record> records) {
		this.records = records;
	}

	@Override
	public int getRowCount() {
		return records.size();
	}

	@Override
	public int getColumnCount() {
		return Field.values().length + 1;
	}
	

	@Override
	public String getColumnName(int column) {
		
		if (column == 0)
			return "Table rows";
		
		return Field.values()[column - 1].getName();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		
		Record rec = records.get(rowIndex);
		if (columnIndex == 0)
			return  rec.getRowStart();
		
		
		Field field = Field.values()[columnIndex - 1];
		
		Object o =  rec.getValue(field);
		
		if (o instanceof String) {
			
		}
		
		
		return o;
	}

}
