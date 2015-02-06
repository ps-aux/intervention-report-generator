package sk.lkce.techforms;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.AcroFields;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;

public class ReportGenerator {

	private static final int FIRST_ROW = 9;
	private static final int FIRST_COL = 0;
	private static final int LAST_COL = 20;
	private static final int PROJECT_FIRST_ROW = 1;
	private static final int PROJECT_FIRST_COL = 1;

	private static final int FIRMY_FIRST_ROW = 1;
	private static final int FIRMY_FIRST_COL = 0;
	private static final int FIRMY_MAX_EMPTY_ROWS = 10;
	private static final int MAIN_SHEET_INDEX = 0;
	private static final int FIRMY_SHEET_INDEX = 1;
	private static final String SEPARATOR = System
			.getProperty("file.separator");

	private static final String PDF_NAME = "form.pdf";

	private static final DateFormat DATE_FORMAT = new SimpleDateFormat(
			"dd/MM/yyyy");

	private Calendar calendar = new GregorianCalendar();
	private Map<String, String> projectMap;
	private Map<String, FirmyRecord> firmyMap;
	private Map<String, Map<Integer, Integer>> reportNoMapAll = new HashMap<>();

	private Sheet mainSheet;
	private Sheet firmySheet;
	private Workbook workbook;

	private String inputFile, outputDir;
	private PdfReader pdfReader;
	private int recordCount;
	private ReportGeneratorListener listener;
	private List<String> usedFileNames;

	public ReportGenerator(String inputFile, String outputDir,
			ReportGeneratorListener listener) {
		this.inputFile = inputFile;
		this.outputDir = outputDir;
		this.listener = listener;

	}

	private void initExcel() throws IOException {
		listener.statusMsgChanged("Initializing...");
		workbook = new XSSFWorkbook(inputFile);
		mainSheet = workbook.getSheetAt(MAIN_SHEET_INDEX);
		firmySheet = workbook.getSheetAt(FIRMY_SHEET_INDEX);

		projectMap = readProjectMap();
		firmyMap = readFirmyMap();
	}

	private void initPdf() throws IOException {
		URL pdfUrl = getClass().getResource(PDF_NAME);
		pdfReader = new PdfReader(pdfUrl);
	}

	public void parseAndProcessAll() throws IOException, COSVisitorException,
			DocumentException {

		initExcel();
		initPdf();
		usedFileNames = new ArrayList<>();

		listener.messageIssued("Starting report generation from the file '"
				+ inputFile + "' to the directory '" + outputDir + "'");

		listener.statusMsgChanged("Starting parsing table...");
		int row = FIRST_ROW;
		int col = 0;
		listener.messageIssued("Parsing & processing records from the table at sheet with index "
				+ MAIN_SHEET_INDEX + ", row " + row + " and column " + col);
		RecordParser recParser = new RecordParser(row, col, mainSheet);
		Record rec;
		while ((rec = recParser.parseNext()) != null) {
			processRecord(rec);
		}

		lastOkRow = recParser.getLastParsedRow() + 1;

		workbook.close();
		listener.messageIssued("Report generation finished. Number of generated report is "
				+ recordCount);
		listener.messageIssued("Last found record was at row "
				+ (recParser.getLastParsedRow() + 1));
		listener.messageIssued("------------------------------------");
		listener.statusMsgChanged("Finished!");
	}

	public void processRecords(Collection<Record> records) throws IOException,
			COSVisitorException, DocumentException {

		usedFileNames = new ArrayList<>();
		List<Integer> indexes = new ArrayList<>();

		for (Record rec : records)
			indexes.add(rec.getRowStart());

		listener.messageIssued("Starting processing selected records from rows "
				+ indexes);
		initPdf();
		for (Record rec : records)
			processRecord(rec);

		listener.messageIssued("Generation of selected reports successfully finished");
		listener.messageIssued("------------------------------------");
	}

	public List<Record> parseRecords() throws IOException {

		initExcel();

		int row = FIRST_ROW;
		int col = FIRST_COL;
		listener.messageIssued("Parsing records from the table at sheet with index "
				+ MAIN_SHEET_INDEX + ", row " + row + " and column " + col);
		RecordParser recParser = new RecordParser(row, col, mainSheet);
		Record rec;
		List<Record> recs = new ArrayList<>();
		while ((rec = recParser.parseNext()) != null)
			recs.add(rec);

		listener.messageIssued("Parsed " + recs.size()
				+ " records. Last parsed row: "
				+ (recParser.getLastParsedRow() + 1));

		workbook.close();

		return recs;
	}

	private class RecordParser {

		private int row, column;
		private int recStart;
		private Sheet sheet;
		private boolean tableEnd;

		public RecordParser(int row, int column, Sheet sheet) {
			this.row = row;
			recStart = row;
			this.column = column;
			this.sheet = sheet;
		}

		public Record parseNext() {
			if (tableEnd)
				return null;
			while (!tableEnd) {
				row++;
				String val = getCellVal(row, column, sheet);
				if (val.isEmpty()) {
					if (isRowEmpty(row)) {
						// We hit the bottom of the table. Row now points to the
						// first row after the table.
						tableEnd = true;
						// Last record
						return readRecord(recStart, row - 1);
					}
				} else {
					// Row now points to the start of the next record
					Record record = readRecord(recStart, row - 1);
					recStart = row; // This will be the start of the next record
					return record;
				}
			}

			// We hit end of the table
			return null;
		}

		private boolean isRowEmpty(int row) {
			for (int col = 0; col <= LAST_COL; col++) {
				String val = getCellVal(row, col, sheet);
				if (!val.isEmpty())
					return false;
			}
			return true;
		}

		public int getLastParsedRow() {
			return recStart;
		}

	}

	/**
	 * Reads the record at the given row range. For one-row record the start and
	 * end row indexes are the same.
	 * 
	 * @param rowStart
	 *            row index where record start - inclusive
	 * @param rowEnd
	 *            row index where record starts - inclusive
	 * @return new record object
	 */
	private Record readRecord(int rowStart, int rowEnd) {
		if (rowStart > rowEnd)
			throw new IllegalArgumentException(
					"Start index is greater than end index");
		listener.statusMsgChanged("Parsing record at rows " + rowStart + " - "
				+ rowEnd);
		Record record = new Record(rowStart + 1, rowEnd + 1);

		String company = getCellVal(rowStart, Field.COMPANY.getColumn(),
				mainSheet);
		String key = company.trim().toLowerCase();
		FirmyRecord firmyRecord = firmyMap.get(key);
		String rowStr = "Row: " + (rowStart + 1) + " - " + (rowEnd + 1);
		if (firmyRecord != null)
			company = firmyMap.get(key).getFullName();
		else
			warn(rowStr
					+ ": No corresponding company record found for the company '"
					+ company + "'");
		record.setValue(Field.COMPANY, company);

		int year = getYear(rowStart);
		String intervReportNo;
		if (year != 0) {
			Map<Integer, Integer> reportNoMap = reportNoMapAll.get(key);
			Integer reportNo;
			if (reportNoMap == null) {
				Map<Integer, Integer> map = new HashMap<>();
				reportNoMapAll.put(key, map);
				map.put(year, 1);
				reportNo = 1;
			} else {
				reportNo = reportNoMap.get(year);
				if (reportNo == null) {
					reportNoMap.put(year, 1);
					reportNo = 1;
				} else {
					reportNo++;
					reportNoMap.put(year, reportNo);
				}
			}
			intervReportNo = year + String.format("%04d", reportNo);
		} else { // There is no year specified
			// Could no generate report number = no report number
			intervReportNo = "";
		}

		record.setValue(Field.REPORT_NO, intervReportNo);

		String tName, tPhone, tEmail;
		if (firmyRecord != null) {
			tName = firmyRecord.getTechnicianName();
			tPhone = firmyRecord.getTechnicianPhone();
			tEmail = firmyRecord.getTechnicianEmail();
		} else {
			warn(rowStr
					+ ": Could not determine technician details without the company record. Empty string will be set.");
			tName = tPhone = tEmail = "";
		}

		record.setValue(Field.TECHNICIAN_NAME, tName);
		record.setValue(Field.TECHNICIAN_EMAIL, tEmail);
		record.setValue(Field.TECHNICIAN_PHONE, tPhone);

		// Contact
		String contact = getCellVal(rowStart, Field.CONTACT.getColumn(),
				mainSheet);
		record.setValue(Field.CONTACT, contact);

		// Address
		String address = getCellVal(rowStart, Field.ADDRESS.getColumn(),
				mainSheet).trim();
		String projectKey = address.trim().toLowerCase();

		if (projectMap.containsKey(projectKey))
			address = projectMap.get(projectKey);
		record.setValue(Field.ADDRESS, address);
		record.setProject(address);

		// Maintenance
		boolean corrective = getCellVal(rowStart, Field.CORRECTIVE.getColumn(),
				mainSheet).toLowerCase().equals("yes");
		boolean preventive = getCellVal(rowStart, Field.PREVENTIVE.getColumn(),
				mainSheet).toLowerCase().equals("yes");
		boolean improvement = getCellVal(rowStart,
				Field.IMPROVEMENT.getColumn(), mainSheet).toLowerCase().equals(
				"yes");

		record.setValue(Field.CORRECTIVE, corrective);
		record.setValue(Field.PREVENTIVE, preventive);
		record.setValue(Field.IMPROVEMENT, improvement);

		// Object of intervention
		String objOfIntv = getCellVal(rowStart,
				Field.OBJECT_OF_INTERVENTION.getColumn(), mainSheet);
		record.setValue(Field.OBJECT_OF_INTERVENTION, objOfIntv);

		// Date
		// TODO handle
		String date;
		double num = mainSheet.getRow(rowStart).getCell(Field.DATE.getColumn())
				.getNumericCellValue();

		if (num == 0)
			date = "";
		else {

			Date date1 = DateUtil.getJavaDate(num);
			Date date2 = null;

			Cell dateToCell = mainSheet.getRow(rowStart).getCell(
					Field.DATE.getColumn() + 1);
			if (dateToCell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
				double toDateNum = dateToCell.getNumericCellValue();
				date2 = DateUtil.getJavaDate(toDateNum);
			}

			date = DATE_FORMAT.format(date1);
			if (date2 != null) {
				date += " - " + DATE_FORMAT.format(date2);
				if (date1.compareTo(date2) > 0)
					warn(rowStr + ": Date FROM is after the date TO.");
			}
		}
		record.setValue(Field.DATE, date);
		// Arrival & departure
		String arrival = getHourAsString(rowStart, Field.ARRIVAL.getColumn(),
				mainSheet);
		record.setValue(Field.ARRIVAL, arrival);

		String departure = getHourAsString(rowStart,
				Field.DEPARTURE.getColumn(), mainSheet);
		record.setValue(Field.DEPARTURE, departure);

		// Duration
		String duration = getCellVal(rowStart, Field.DURATION.getColumn(),
				mainSheet);
		record.setValue(Field.DURATION, duration);

		// Number of technicians
		String noOfTechnicians = getCellVal(rowStart,
				Field.NO_OF_TECHNICIANS.getColumn(), mainSheet);
		try {
			double noOfTD = Double.parseDouble(noOfTechnicians);
			noOfTechnicians = ((int) noOfTD) + "";
		} catch (NumberFormatException ex) {
			// Nothing to do here. Simply it was not double.
		}

		record.setValue(Field.NO_OF_TECHNICIANS, noOfTechnicians);

		// Stops disturbing the production
		List<String> stops = new ArrayList<>();
		for (int i = rowStart; i <= rowEnd; i++) {
			String stop = getCellVal(i, Field.STOPS.getColumn(), mainSheet);
			if (!stop.trim().isEmpty())
				stops.add(stop);

		}
		record.setValue(Field.STOPS, stops);

		// Description of the intervention
		List<String> descs = new ArrayList<>();
		for (int i = rowStart; i <= rowEnd; i++) {
			String desc = getCellVal(i, Field.DESCRIPTION.getColumn(),
					mainSheet);
			if (!desc.trim().isEmpty())
				descs.add(desc);
		}
		record.setValue(Field.DESCRIPTION, descs);

		// Spare parts reference
		String reference = getCellVal(rowStart, Field.REFERENCE.getColumn(),
				mainSheet);
		record.setValue(Field.REFERENCE, reference);

		// Spare parts quantity
		String quantity = getCellVal(rowStart, Field.QUANTITY.getColumn(),
				mainSheet);
		record.setValue(Field.QUANTITY, quantity);

		// Spare parts unitPrice
		String unitPrice = getCellVal(rowStart, Field.UNIT_PRICE.getColumn(),
				mainSheet);
		record.setValue(Field.UNIT_PRICE, unitPrice);

		// Billable
		boolean billable = getCellVal(rowStart, Field.BILLABLE.getColumn(),
				mainSheet).toLowerCase().equals("yes");
		record.setValue(Field.BILLABLE, billable);

		// Invoicing
		boolean included = getCellVal(rowStart,
				Field.INCLUDED_IN_CONTRACT.getColumn(), mainSheet)
				.toLowerCase().equals("yes");
		record.setValue(Field.INCLUDED_IN_CONTRACT, included);

		return record;
	}

	private static String getHourAsString(int row, int column, Sheet sheet) {
		Cell cell = sheet.getRow(row).getCell(column);

		if (cell.getCellType() != Cell.CELL_TYPE_NUMERIC)
			return "";

		double num = cell.getNumericCellValue();

		Calendar cal = DateUtil.getJavaCalendar(num);
		String hour = String.format("%02d", cal.get(Calendar.HOUR_OF_DAY))
				+ ":" + String.format("%02d", cal.get(Calendar.MINUTE));

		return hour;
	}

	private int getYear(int row) {
		Cell cell = mainSheet.getRow(row).getCell(Field.REPORT_NO.getColumn());
		double dateNum = cell.getNumericCellValue();

		// Missing date
		if (dateNum == 0)
			return 0;

		Date date = DateUtil.getJavaDate(dateNum);
		calendar.setTime(date);
		return calendar.get(Calendar.YEAR);
	}

	private void processRecord(Record record) throws IOException,
			COSVisitorException, DocumentException {

		String project = record.getProject();
		if (project.isEmpty())
			project = "unknown_project";

		String dir = outputDir + SEPARATOR + project;

		// Ensure dir is created
		File d = new File(dir);
		if (!d.exists())
			d.mkdir();

		String repNo = (String) record.getValue(Field.REPORT_NO);
		repNo = repNo.trim();
		if (repNo.isEmpty())
			repNo = "no_number";

		String date = (String) record.getValue(Field.DATE);
		date = date.trim();
		date = date.replaceAll("/", "_");
		date = date.replaceAll(" ", "");
		String name = repNo + "-" + project + "-" + date + ".pdf";

		String path = dir + SEPARATOR + name;

		// Ensure the file name is not taken by another file from this batch
		// so it is not overwritten
		if (usedFileNames.contains(path)) {
			int suffix = 1;
			String modPath = path + "-" + suffix;
			while (usedFileNames.contains(modPath)) {
				suffix++;
				modPath = path + "-" + suffix;
			}
			path = modPath;
		}

		PdfReader copyReader = new PdfReader(pdfReader);
		listener.statusMsgChanged("Creating pdf file " + path);
		PdfStamper pdfStamper = new PdfStamper(copyReader,
				new FileOutputStream(path));

		AcroFields acroFields = pdfStamper.getAcroFields();

		for (Field fieldType : Field.values()) {
			Object val = record.getValue(fieldType);
			String fieldVal = val.toString();

			if (fieldType.getType() == Boolean.class) {
				Boolean b = (Boolean) val;
				if (b)
					fieldVal = "Yes";
				else
					fieldVal = "No";
			}

			if (fieldType == Field.STOPS) {
				@SuppressWarnings("unchecked")
				List<String> rows = (List<String>) val;

				for (int i = 1; i <= rows.size(); i++) {
					String fieldName = "Stops" + i;
					if (i <= 7)
						setField(acroFields, fieldName, rows.get(i - 1));
					else
						warn("Will not set Stops" + i
								+ " field for the record at rows "
								+ record.getRowStart() + " - "
								+ record.getRowEnd()
								+ " - the number of lines is greater than 7");
				}
			} else if (fieldType == Field.DESCRIPTION) {
				@SuppressWarnings("unchecked")
				List<String> rows = (List<String>) val;

				for (int i = 1; i <= rows.size(); i++) {
					String fieldName = "Description" + i;
					if (i <= 14)
						setField(acroFields, fieldName, rows.get(i - 1));
					else
						warn("Will not set Description" + i
								+ " field for the record at row "
								+ record.getRowStart() + " - "
								+ record.getRowEnd()
								+ " - the number of lines is greater than 14");
				}

			} else {
				setField(acroFields, fieldType.getName(), fieldVal);
			}
		}

		recordCount++;
		pdfStamper.close();
	}

	private void setField(AcroFields fields, String name, String val)
			throws IOException, DocumentException {

		boolean wasSet = fields.setField(name, val);
		if (!wasSet)
			throw new IllegalStateException("Field '" + name
					+ "'could not be set");
	}

	private static String getCellVal(int row, int col, Sheet sheet) {
		Row sheetRow = sheet.getRow(row);

		if (sheetRow == null)
			return "";
		Cell cell = sheetRow.getCell(col);
		if (cell == null)
			return "";

		switch (cell.getCellType()) {
		case Cell.CELL_TYPE_BLANK:
			return "";
		case Cell.CELL_TYPE_BOOLEAN:
			return Boolean.toString(cell.getBooleanCellValue());
		case Cell.CELL_TYPE_ERROR:
			return Byte.toString(cell.getErrorCellValue());
		case Cell.CELL_TYPE_FORMULA:
			return "#formula";
		case Cell.CELL_TYPE_NUMERIC:
			return Double.toString(cell.getNumericCellValue());
		case Cell.CELL_TYPE_STRING:
			return cell.getStringCellValue();
		default:
			throw new IllegalStateException("Unknow cell type");
		}
	}

	private Map<String, String> readProjectMap() {
		String val1;
		String val2;

		int row = PROJECT_FIRST_ROW;
		Map<String, String> map = new HashMap<>();

		do {
			val1 = getCellVal(row, PROJECT_FIRST_COL, mainSheet);
			val2 = getCellVal(row, PROJECT_FIRST_COL + 1, mainSheet);
			row++;

			if (!val1.isEmpty())
				map.put(val1.trim().toLowerCase(), val2);
		} while (!val1.isEmpty());

		return map;
	}

	private Map<String, FirmyRecord> readFirmyMap() {

		int row = FIRMY_FIRST_ROW;
		int col = FIRMY_FIRST_COL;
		Map<String, FirmyRecord> map = new HashMap<>();
		int emptyRowsCount = 0;

		while (emptyRowsCount <= FIRMY_MAX_EMPTY_ROWS) {
			String val = getCellVal(row, col, firmySheet);
			if (val.isEmpty())
				emptyRowsCount++;
			else {
				// Trimmed value without taking case into consideration
				map.put(val.trim().toLowerCase(), readFirmyRecord(row));
				emptyRowsCount = 0;
			}

			row++;
		}

		return map;
	}

	private FirmyRecord readFirmyRecord(int row) {
		FirmyRecord record = new FirmyRecord();

		int col = FIRMY_FIRST_COL;
		record.setShortName(getCellVal(row, col, firmySheet));
		col += 2;

		int myRow = row;
		String val = getCellVal(myRow, col, firmySheet);
		StringBuilder fullName = new StringBuilder();
		fullName.append(val);
		myRow++;
		while (!val.isEmpty()) {
			val = getCellVal(myRow, col, firmySheet);
			fullName.append("\n").append(val);
			myRow++;
		}

		record.setFullName(fullName.toString());

		col += 2;
		myRow = row;

		record.setTechnicianName(getCellVal(myRow++, col, firmySheet));
		record.setTechnicianPhone(getCellVal(myRow++, col, firmySheet));
		record.setTechnicianEmail(getCellVal(myRow, col, firmySheet));

		return record;
	}

	private void warn(String msg) {
		listener.warningIssued(msg);
	}

}
