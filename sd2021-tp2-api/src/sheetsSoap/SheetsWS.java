package sheetsSoap;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import extras.Connection;
import extras.Discovery;
import jakarta.jws.WebService;
import tp1.api.Spreadsheet;
import tp1.api.service.soap.SheetsException;
import tp1.api.service.soap.SoapSpreadsheets;
import tp1.impl.engine.AbstractSpreadSheetImpl;
import tp1.impl.engine.SpreadsheetEngineImpl;

@WebService(serviceName = SoapSpreadsheets.NAME, targetNamespace = SoapSpreadsheets.NAMESPACE, endpointInterface = SoapSpreadsheets.INTERFACE)
public class SheetsWS implements SoapSpreadsheets {

	private static Logger Log = Logger.getLogger(SheetsWS.class.getName());

	private String domain;
	private final Map<String, Spreadsheet> sheets = new HashMap<String, Spreadsheet>();
	private final Map<String, String[][]> cache = new HashMap<String, String[][]>();
	private Discovery d;
	private int id = 0;
	private String ip;
	private Connection c;
	private String secret;

	public SheetsWS(String domain, Discovery d, String ip, String secret) {
		this.domain = domain;
		this.d = d;
		this.ip = ip;
		this.secret = secret;
		c = new Connection(secret);

	}

	@Override
	public String createSpreadsheet(Spreadsheet sheet, String password) throws SheetsException {
		if (sheet.getOwner() == null || sheet.getRows() <= 0 || sheet.getColumns() <= 0) {
			Log.info("something bad.");
			throw new SheetsException("something bad.");
		}
		synchronized (this) {
			sheet.setSheetId("" + id);
			sheet.setSheetURL("https://" + ip + ":" + 8080 + "/soap/" + NAME + "/?wsdl/" + id);
			id++;
			Log.info("createSheet : " + sheet + "\n");

			getUser(sheet.getOwner(), password);
			
			

			Log.info("sheet created.\n");
			sheets.put(sheet.getSheetId(), sheet);
		}

		return sheet.getSheetId();
	}

	@Override
	public void deleteSpreadsheet(String sheetId, String password) throws SheetsException {
		Log.info("deleteSheet : sheet = " + sheetId + "; pwd = " + password + "\n");

		if (sheetId == null) {
			Log.info("SheetId null.");
			throw new SheetsException("SheetId null.");
		}

		if (password == null) {
			Log.info("password null.");
			throw new SheetsException("password null.");
		}
		Spreadsheet s;
		synchronized (this) {
			s = sheets.get(sheetId);

			if (s == null) {
				Log.info("Sheet null.");
				throw new SheetsException("Sheet null.");
			}
		}

		getUser(s.getOwner(), password);
		
		synchronized (this) {
			sheets.remove(sheetId, s);
		}

	}

	@Override
	public Spreadsheet getSpreadsheet(String sheetId, String userId, String password) throws SheetsException {
		Log.info("getSheet:sheet = " + sheetId + "; user = " + userId + "; pwd = " + password + "\n");

		if (sheetId == null) {
			Log.info("SheetId null.\n");
			throw new SheetsException("sheetId null.");
		}

		if (userId == null) {
			Log.info("userId null.\n");
			throw new SheetsException("userId null.");
		}

		if (password == null) {
			Log.info("password null.\n");
			throw new SheetsException("password null.");
		}

		Spreadsheet s;
		synchronized (this) {
			s = sheets.get(sheetId);
		}

		if (s == null) {
			Log.info("Sheet null.\n");
			throw new SheetsException("sheet null.");
		}

		boolean isOwner = false;
		boolean isShared = false;
		synchronized (this) {
			isOwner = s.getOwner().equals(userId);
			if (s.getSharedWith() == null) {
				isShared = false;
			} else {
				isShared = s.getSharedWith().contains(userId + "@" + domain);
			}

		}

		if (isOwner || isShared) {

			getUser(userId, password);
				

		} else {
			Log.info("user null.\n");
			throw new SheetsException("user null.");
		}

		return s;
	}

	@Override
	public void shareSpreadsheet(String sheetId, String userId, String password) throws SheetsException {
		Log.info("shareSheet : sheet = " + sheetId + "; user = " + userId + "; pwd = " + password + "\n");

		if (sheetId == null) {
			Log.info("SheetId null.");
			throw new SheetsException("sheetId null.");
		}

		if (password == null) {
			Log.info("password null.");
			throw new SheetsException("password null.");
		}

		Spreadsheet s;
		synchronized (this) {
			s = sheets.get(sheetId);
		}

		if (s == null) {
			Log.info("Sheet null.");
			throw new SheetsException("sheet null.");
		}

		getUser(s.getOwner(), password);

		
		synchronized (this) {
			if (s.getSharedWith() == null) {
				s.setSharedWith(new HashSet<String>());
				s.getSharedWith().add(userId);
			} else if (s.getSharedWith().contains(userId)) {
				Log.info("already shared");
				throw new SheetsException("already shared");
			} else {

				s.getSharedWith().add(userId);
			}
		}

	}

	@Override
	public void unshareSpreadsheet(String sheetId, String userId, String password) throws SheetsException {
		Log.info("unshareSheet : sheet = " + sheetId + "; user = " + userId + "; pwd = " + password + "\n");

		if (sheetId == null) {
			Log.info("SheetId null.");
			throw new SheetsException("sheetId null.");
		}

		if (userId == null) {
			Log.info("userId null.");
			throw new SheetsException("userId null.");
		}

		if (password == null) {
			Log.info("password null.");
			throw new SheetsException("password null.");
		}

		Spreadsheet s;
		synchronized (this) {
			s = sheets.get(sheetId);
		}

		if (s == null) {
			Log.info("Sheet null.");
			throw new SheetsException("sheet null.");
		}

		getUser(s.getOwner(), password);

		synchronized (this) {
			if (s.getSharedWith().contains(userId) && s.getSharedWith() != null) {
				s.getSharedWith().remove(userId);

			} else {
				throw new SheetsException("not shared");
			}
		}

	}

	@Override
	public void updateCell(String sheetId, String cell, String rawValue, String userId, String password)
			throws SheetsException {
		Log.info("updateCell : sheet = " + sheetId + "; cell = " + cell + "; rawValue = " + rawValue + "; user = "
				+ userId + "; pwd = " + password + "\n");

		if (sheetId == null || cell == null || rawValue == null || userId == null) {
			Log.info("SheetId null.");
			throw new SheetsException("sheetId null.");
		}

		if (password == null) {
			Log.info("password null");
			throw new SheetsException("password null.");
		}

		Spreadsheet s;
		synchronized (this) {
			s = sheets.get(sheetId);
		}

		if (s == null) {
			Log.info("Sheet null.");
			throw new SheetsException("sheet null.");
		}

		getUser(userId, password);

		synchronized (this) {
			s.setCellRawValue(cell, rawValue);
		}

	}

	@Override
	public String[][] getSpreadsheetValues(String sheetId, String userId, String password) throws SheetsException {
		Log.info("getSheetValues:sheet = " + sheetId + "; user = " + userId + "; pwd = " + password + "\n");

		if (sheetId == null) {
			Log.info("SheetId null.\n");
			throw new SheetsException("sheetId null.");
		}

		if (userId == null) {
			Log.info("userId null.\n");
			throw new SheetsException("userId null.");
		}

		if (password == null) {
			Log.info("password null.\n");
			throw new SheetsException("password null.");
		}

		Spreadsheet s;
		synchronized (this) {
			s = sheets.get(sheetId);
		}

		if (s == null) {
			Log.info("Sheet null.\n");
			throw new SheetsException("sheet null.");
		}

		boolean isOwner = false;
		boolean isShared = false;
		synchronized (this) {
			isOwner = s.getOwner().equals(userId);
			if (s.getSharedWith() == null) {
				isShared = false;
			} else {
				isShared = s.getSharedWith().contains(userId + "@" + domain);
			}
		}

		if (isOwner || isShared) {

			getUser(userId, password);

		} else {
			Log.info("user null.\n");
			throw new SheetsException("user null.");
		}

		return SpreadsheetEngineImpl.getInstance().computeSpreadsheetValues(new AbstractSpreadSheetImpl(s, domain, cache, sheets, ip + ":8080", c));
	}

	private void getUser(String userId, String password) throws SheetsException {
		URI serviceUri = d.knownUrisOf(domain + ":users");
		try {
		c.getUses(serviceUri, userId, password);
		}catch (Exception e) {
			Log.info("user does not exist.\n");
			throw new SheetsException("user does not exist");
		}
	}

	@Override
	public void deleteUserSpreadsheet(String userId, String secret) throws SheetsException {
		Log.info("deleting " + userId + " sheets\n");
		
		if (secret.equals(this.secret)) {
			synchronized (this) {

				Iterator<Spreadsheet> i = sheets.values().iterator();

				while (i.hasNext()) {
					Spreadsheet s = i.next();

					if (s.getOwner().equals(userId)) {
						i.remove();

					}
				}
			}
		}
		

	}

	@Override
	public String[][] getImportValues(String sheetId, String userId, String secret) throws SheetsException {
		Log.info("importvalues = " + sheetId + "   user = " + userId + "\n");
		Spreadsheet s = sheets.get(sheetId);
		if (s.getSharedWith().contains(userId) && secret.equals(this.secret)) {
			return SpreadsheetEngineImpl.getInstance().computeSpreadsheetValues(new AbstractSpreadSheetImpl(s, domain, cache, sheets, ip + ":8080", c));
		}

		return null;
	}

}
