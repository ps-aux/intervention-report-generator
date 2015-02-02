package sk.lkce.techforms;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
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

/**
 * Hello world!
 *
 */
public class ReportGenerator {

	private static final int FIRST_ROW = 9;
	private static final int PROJECT_FIRST_ROW = 1;
	private static final int PROJECT_FIRST_COL = 1;

	private static final int FIRMY_FIRST_ROW = 1;
	private static final int FIRMY_FIRST_COL = 0;
	private static final int FIRMY_MAX_EMPTY_ROWS = 10;
	private static final int MAIN_MAX_EMPTY_ROWS = 40;
	private static final int MAIN_SHEET_INDEX = 0;
	private static final int FIRMY_SHEET_INDEX = 1;

	private static final String PDF_NAME = "form.pdf";

	private static final DateFormat DATE_FORMAT = new SimpleDateFormat(
			"dd/MM/yyyy");

	private Calendar calendar = new GregorianCalendar();
	private Map<String, String> projectMap;
	private Map<String, FirmyRecord> firmyMap;
	private Map<String, Map<Integer, Integer>> reportNoMapAll = new HashMap<>();

	private Sheet mainSheet;
	private Sheet firmySheet;

	private String inputFile, outputDir;
	private PdfReader pdfReader;
	private int recordCount;
	private int fileNo = 1;
	private ReportGeneratorListener listener;

	public ReportGenerator(String inputFile, String outputDir, ReportGeneratorListener listener) {
		this.inputFile = inputFile;
		this.outputDir = outputDir;
		this.listener = listener;

	}

	public void go() throws IOException, COSVisitorException, DocumentException {
		listener.statusMsgChanged("Initializing...");
		Workbook workbook = new XSSFWorkbook(inputFile);
		mainSheet = workbook.getSheetAt(MAIN_SHEET_INDEX);
		firmySheet = workbook.getSheetAt(FIRMY_SHEET_INDEX);

		projectMap = readProjectMap();
		firmyMap = readFirmyMap();

		URL pdfUrl = getClass().getResource(PDF_NAME);
		pdfReader = new PdfReader(pdfUrl);

		
		listener.messageIssued("Starting report generation from the file '" + inputFile 
				+ "' to the directory '" + outputDir + "'");
		
		listener.statusMsgChanged("Starting parsing table...");
		int row = FIRST_ROW;
		int col = 0;
		int emptyRowsCount = 0;
		int lastOkRow = -1;
		listener.messageIssued("Processing records from the table at sheet with index " + MAIN_SHEET_INDEX + ", row " +
				row + " and column " + col);
		while (emptyRowsCount <= MAIN_MAX_EMPTY_ROWS) {
			String val = getCellVal(row, col, mainSheet);
			if (val.isEmpty())
				emptyRowsCount++;
			else {
				lastOkRow = row;
				Record record = readRecord(row);
				processRecord(record);
				emptyRowsCount = 0;
			}

			row++;
		}

		listener.messageIssued("Report generation finished. Number of generated report: " + recordCount);
		listener.messageIssued("Last row in the sheet which was taken "
				+ "into consideration:" + lastOkRow + ".");
		listener.statusMsgChanged("Finished!");
		workbook.close();
	}

	private Record readRecord(int row) {
		listener.statusMsgChanged("Parsing record at row " + row);
		Record record = new Record();

		String company = getCellVal(row, Field.COMPANY.getColumn(), mainSheet);
		String key = company.trim().toLowerCase();
		FirmyRecord firmyRecord = firmyMap.get(key);
		String rowStr = "Row: " + (row + 1);
		if (firmyRecord != null)
			company = firmyMap.get(key).getFullName();
		else
			warn(rowStr
					+ ": No corresponding company record found for the company '"
					+ company + "'");
		record.setValue(Field.COMPANY, company);

		int year = getYear(row);

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

		String intervReportNo = year + String.format("%04d", reportNo);
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
		String contact = getCellVal(row, Field.CONTACT.getColumn(), mainSheet);
		record.setValue(Field.CONTACT, contact);

		// Address
		String address = getCellVal(row, Field.ADDRESS.getColumn(), mainSheet);
		String projectKey = address.trim().toLowerCase();

		if (projectMap.containsKey(projectKey))
			address = projectMap.get(projectKey);
		record.setValue(Field.ADDRESS, address);

		// Maintenance
		boolean corrective = getCellVal(row, Field.CORRECTIVE.getColumn(),
				mainSheet).toLowerCase().equals("yes");
		boolean preventive = getCellVal(row, Field.PREVENTIVE.getColumn(),
				mainSheet).toLowerCase().equals("yes");
		boolean improvement = getCellVal(row, Field.IMPROVEMENT.getColumn(),
				mainSheet).toLowerCase().equals("yes");

		record.setValue(Field.CORRECTIVE, corrective);
		record.setValue(Field.PREVENTIVE, preventive);
		record.setValue(Field.IMPROVEMENT, improvement);

		// Object of intervention
		String objOfIntv = getCellVal(row,
				Field.OBJECT_OF_INTERVENTION.getColumn(), mainSheet);
		record.setValue(Field.OBJECT_OF_INTERVENTION, objOfIntv);

		// Date
		// TODO handle
		Date date1 = DateUtil.getJavaDate(mainSheet.getRow(row)
				.getCell(Field.DATE.getColumn()).getNumericCellValue());
		Date date2 = null;

		Cell dateToCell = mainSheet.getRow(row).getCell(
				Field.DATE.getColumn() + 1);
		if (dateToCell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
			double toDateNum = dateToCell.getNumericCellValue();
			date2 = DateUtil.getJavaDate(toDateNum);
		}

		warn(rowStr + ": Date FROM is after the date TO.");

		String date = DATE_FORMAT.format(date1);
		if (date2 != null)
			date += " - " + DATE_FORMAT.format(date2);

		record.setValue(Field.DATE, date);
		// Arrival & departure
		String arrival = getHourAsString(row, Field.ARRIVAL.getColumn(),
				mainSheet);
		record.setValue(Field.ARRIVAL, arrival);

		String departure = getHourAsString(row, Field.DEPARTURE.getColumn(),
				mainSheet);
		record.setValue(Field.DEPARTURE, departure);

		// Duration
		String duration = getCellVal(row, Field.DURATION.getColumn(), mainSheet);
		record.setValue(Field.DURATION, duration);

		// Number of technicians
		String noOfTechnicians = getCellVal(row,
				Field.NO_OF_TECHNICIANS.getColumn(), mainSheet);
		try {
			double noOfTD = Double.parseDouble(noOfTechnicians);
			noOfTechnicians = ((int) noOfTD) + "";
		} catch (NumberFormatException ex) {
			// Nothing to do here. Simply it was not double.
		}

		record.setValue(Field.NO_OF_TECHNICIANS, noOfTechnicians);

		// Stops disturbing the production
		String stops = getCellVal(row, Field.STOPS.getColumn(), mainSheet);
		record.setValue(Field.STOPS, stops.split("/n"));

		// Description of the intervention
		String desc = getCellVal(row, Field.DESCRIPTION.getColumn(), mainSheet);
		String[] lines = desc.split("/n");
		record.setValue(Field.DESCRIPTION, lines);

		// Spare parts reference
		String reference = getCellVal(row, Field.REFERENCE.getColumn(),
				mainSheet);
		record.setValue(Field.REFERENCE, reference);

		// Spare parts quantity
		String quantity = getCellVal(row, Field.QUANTITY.getColumn(), mainSheet);
		record.setValue(Field.QUANTITY, quantity);

		// Spare parts unitPrice
		String unitPrice = getCellVal(row, Field.UNIT_PRICE.getColumn(),
				mainSheet);
		record.setValue(Field.UNIT_PRICE, unitPrice);

		// Billable
		boolean billable = getCellVal(row, Field.BILLABLE.getColumn(),
				mainSheet).toLowerCase().equals("yes");
		record.setValue(Field.BILLABLE, billable);

		// Invoicing
		boolean included = getCellVal(row,
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

		// if (dateNum == 0)
		// throw new IllegalArgumentException("The date at row " + (row +1) +
		// " is 0 (probably missing value)");

		Date date = DateUtil.getJavaDate(dateNum);
		calendar.setTime(date);
		return calendar.get(Calendar.YEAR);
	}

	private void processRecord(Record record) throws IOException,
			COSVisitorException, DocumentException {

		String path = outputDir + "report_" + fileNo++ + ".pdf";
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
				String[] rows = (String[]) val;

				for (int i = 1; i <= rows.length; i++) {
					String fieldName = "Stops" + i;
					if (i < 8)
						setField(acroFields, fieldName, rows[i- 1]);
					else
						warn("Will not set Stops" + i + " field for " + record
								+ ". "
								+ "The number of lines is greater than 7");
				}
			} else if (fieldType == Field.DESCRIPTION) {
				String[] rows = (String[]) val;

				for (int i = 1; i <= rows.length; i++) {
					String fieldName = "Description" + i;
					if (i < 14)
						setField(acroFields, fieldName, rows[i- 1]);
					else
						warn("Will not set Description" + i + " field for "
								+ record + ". "
								+ "The number of lines is greater than 14");
				}

			} else {
				setField(acroFields, fieldType.getName(), fieldVal);
			}
		}

		recordCount++;
		pdfStamper.close();
	}

	private void setField(AcroFields fields, String name, String val) throws IOException, DocumentException {

		boolean wasSet = fields.setField(name, val);
		if (!wasSet)
			throw new IllegalStateException("Field '" + name + "'could not be set");
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
				map.put(val1, val2);
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
