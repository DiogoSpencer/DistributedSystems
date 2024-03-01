package tp1.impl.engine;

import java.util.Map;
import java.util.logging.Logger;


import extras.Connection;
import sheetsDrop.util.DownloadFile;
import tp1.api.Spreadsheet;
import tp1.api.engine.AbstractSpreadsheet;
import tp1.util.CellRange;

public class AbstractSpreadsheetDropImpl implements AbstractSpreadsheet {
	private static Logger Log = Logger.getLogger(AbstractSpreadsheetDropImpl.class.getName());
	private Spreadsheet sheet;
	private String domain, ip;
	private final Map<String, String[][]> cache;
	private Connection connection;

	public AbstractSpreadsheetDropImpl(Spreadsheet sheet, String domain, Map<String, String[][]> cache, String ip, Connection connection) {
		this.sheet = sheet;
		this.domain = domain;
		this.cache = cache;
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

			String[][] s ;
			
			String[] aux = sheetURL.split("/");
			if (ip.equals(aux[2])) {
				s = SpreadsheetEngineImpl.getInstance().computeSpreadsheetValues(new AbstractSpreadsheetDropImpl((new DownloadFile().execute(aux[aux.length -1], domain)), domain, cache, ip, connection));
				
			}else if(aux[2].equals("content.dropboxapi.com")){
				s = null;
			}else {
				s = connection.importRange(sheetURL, sheet.getOwner()+ "@" + domain);
				if (s == null) {
					s = cache.get(sheetURL);
				} else {
					if (cache.containsKey(sheetURL)) {
						cache.replace(sheetURL, s);
					}else {
						cache.put(sheetURL, s);
					}
				}
			}
					
			for (int i = 0; i < c.rows(); i++) {
				for (int j = 0; j < c.cols(); j++) {
					result[i][j] = s[i+ c.topRow][j + c.topCol];
				}
				
				
				
			}

			return result;

	}
}
