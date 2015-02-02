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
			return  rec.getRowStart() + " - " + rec.getRowEnd();
		
		
		Field field = Field.values()[columnIndex - 1];
		
		Object o =  rec.getValue(field);
		
	
		
		if (field == Field.STOPS || field == Field.DESCRIPTION) {
			@SuppressWarnings("unchecked")
			List<String> rows =  (List<String>) o;
			StringBuilder sb = new StringBuilder();
			
			if (rows.size() == 0)
				return "";
			
			for (int i = 0; i < rows.size(); i++) {
				sb.append(rows.get(i));
				if (i < rows.size() -1)
					sb.append(" | ");
			}
			
			return sb.toString();
		}
		
		return o;

	}

}
