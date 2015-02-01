package sk.lkce.techforms;


enum Field {

	COMPANY("Company", 0, String.class),
	REPORT_NO("ReportNo", 8, String.class),
	TECHNICIAN_NAME( "TechnName", -1, String.class),
	TECHNICIAN_EMAIL("TechnEmail", -1, String.class),
	TECHNICIAN_PHONE( "TechnPhone", -1, String.class),
	CONTACT("Contact", 2, String.class),
	ADDRESS("Address", 3, String.class),
	PREVENTIVE( "Preventive", 5, Boolean.class),
	CORRECTIVE("Corrective", 6, Boolean.class),
	IMPROVEMENT( "Improvement", 7, Boolean.class),
	OBJECT_OF_INTERVENTION( "ObjOfIntervention", 2, String.class),
	DATE("Date", 8, String.class),
	ARRIVAL("Arrival", 10, String.class),
	DEPARTURE("Departure", 11, String.class),
	DURATION("Duration", 12, String.class),
	NO_OF_TECHNICIANS("NoOfTechnicians", 13, String.class),
	STOPS("Stops", 14, String[].class),
	DESCRIPTION("Description", 15, String[].class),
	REFERENCE("Reference", 16, String.class),
	QUANTITY("Quantity", 17, String.class),
	UNIT_PRICE("UnitPrice", 18, String.class),
	BILLABLE("Billable", 19, Boolean.class),
	INCLUDED_IN_CONTRACT("InclInContract", 20, Boolean.class),
	;

	private final String name;
	private final int column;
	private final Class<?> type;

	Field(String name, int column, Class<?> type) {
		this.name = name;
		this.column = column;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public int getColumn() {
		return column;
	}
	
	public Class<?> getType() {
		return type;
	}

}