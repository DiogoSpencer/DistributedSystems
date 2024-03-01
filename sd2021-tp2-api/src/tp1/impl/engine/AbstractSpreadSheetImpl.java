package tp1.impl.engine;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import extras.Connection;
import tp1.api.Spreadsheet;
import tp1.api.engine.AbstractSpreadsheet;
import tp1.util.CellRange;

public class AbstractSpreadSheetImpl implements AbstractSpreadsheet {
	private static Logger Log = Logger.getLogger(AbstractSpreadSheetImpl.class.getName());
	private Spreadsheet sheet;
	private String domain, ip;
	private final Map<String, String[][]> cache;
	private final Map<String, Spreadsheet> sheets;
	private Connection connection;

	public AbstractSpreadSheetImpl(Spreadsheet sheet, String domain, Map<String, String[][]> cache,
			Map<String, Spreadsheet> sheets, String ip, Connection connection) {
		this.sheet = sheet;
		this.domain = domain;
		this.cache = cache;
		this.sheets = sheets;
		this.ip = ip;
		this.connection = connection;
	}

	@Override
	public int rows() {
		return sheet.getRows();
	}

	@Override
	public int columns() {
		return sheet.getColumns();
	}

	@Override
	public String sheetId() {
		return sheet.getSheetId();
	}

	@Override
	public String cellRawValue(int row, int col) {
		return sheet.getCellRawValue(row, col);
	}

	@Override
	public String[][] getRangeValues(String sheetURL, String range) {

		Log.info("sheetUrl = " + sheetURL + "  range  = " + range + "\n");

		CellRange c = new CellRange(range);

		String[][] result = new String[c.rows()][c.cols()];

		String[] aux = sheetURL.split("/");
		if (aux[2].equals("sheets.googleapis.com")) {
			List<List<Object>> vr = connection.importRangeGoogle("https://sheets.googleapis.com/v4/spreadsheets/"+ aux[3] + "/values/" + range +"?key=AIzaSyBV35__zT6MJYwSfmW4eXbrltNzg6xzeXo");
			int count1 = 0;
			int count2 = 0;
			
			for (List<Object> list : vr) {
				for (Object s : list) {
					result[count1][count2++] = (String) s;
				} 
				count1++;
				count2 = 0;
			}
		} else {
			String[][] s;
			if (ip.equals(aux[2])) {
				s = SpreadsheetEngineImpl.getInstance().computeSpreadsheetValues(new AbstractSpreadSheetImpl(
						sheets.get(aux[aux.length - 1]), domain, cache, sheets, ip, connection));

			} else {
				s = connection.importRange(sheetURL, sheet.getOwner() + "@" + domain);
				if (s == null) {
					s = cache.get(sheetURL);
				} else {
					if (cache.containsKey(sheetURL)) {
						cache.replace(sheetURL, s);
					} else {
						cache.put(sheetURL, s);
					}
				}
			}

			for (int i = 0; i < c.rows(); i++) {
				for (int j = 0; j < c.cols(); j++) {
					result[i][j] = s[i + c.topRow][j + c.topCol];
				}
			}
		}

		return result;

	}
}
