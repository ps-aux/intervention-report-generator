package sk.lkce.techforms;

import java.util.HashMap;
import java.util.Map;

public class Record {

	private Map<Field, Object> values = new HashMap<>();
	private int rowStart;
	private String project;
	
	public Record(int rowStart) {
		this.rowStart = rowStart;
	}

	public void setValue(Field field, Object val) {
		if (field == null || val == null)
			throw new NullPointerException();
		if (values.containsKey(field))
			throw new IllegalStateException("The value for " + field.getName()
					+ " is already present");

		Class<?> type = field.getType();
		if (!type.isInstance(val))
			throw new IllegalArgumentException(val + " is not an instance of "
					+ type);

		values.put(field, val);
	}
	

	public String getProject() {
		return project;
	}

	public void setProject(String project) {
		this.project = project;
	}

	public Object getValue(Field field) {
		return values.get(field);
	}
	

	public int getRowStart() {
		return rowStart;
	}

	public void setRowStart(int rowStart) {
		this.rowStart = rowStart;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(Record.class.getSimpleName()).append("[");

		for (Field field : Field.values())
			sb.append(field.getName()).append(": ").append(values.get(field))
					.append(", ");
		
		sb.append("]");

		return sb.toString();
	}

}
