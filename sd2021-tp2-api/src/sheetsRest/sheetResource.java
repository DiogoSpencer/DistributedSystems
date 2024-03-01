package sheetsRest;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;
import java.net.URI;

import extras.Cache;
import extras.Connection;
import extras.Discovery;
import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;
import tp1.api.Spreadsheet;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.impl.engine.AbstractSpreadSheetImpl;
import tp1.impl.engine.AbstractSpreadSheetRestImpl;
import tp1.impl.engine.SpreadsheetEngineImpl;

@Singleton
public class sheetResource implements RestSpreadsheets {

	private final Map<String, Spreadsheet> sheets = new HashMap<String, Spreadsheet>();
	private final Map<String, Cache> cache = new HashMap<String, Cache>();
	private String domain;
	private Discovery d;
	private int id = 0;
	private String ip;
	private Connection c;
	private String secret;

	private static Logger Log = Logger.getLogger(sheetResource.class.getName());

	public sheetResource(String domain, Discovery d, String ip, String secret) {
		this.domain = domain;
		this.d = d;
		this.ip = ip;
		this.secret = secret;
		c = new Connection(secret);
	}

	@Override
	public String createSpreadsheet(Spreadsheet sheet, String password) {

		if (sheet.getOwner() == null || sheet.getRows() <= 0 || sheet.getColumns() <= 0) {
			Log.info("something bad.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}
		synchronized (this) {
			sheet.setSheetId("" + id);
			sheet.setSheetURL("https://" + ip + ":" + 8080 + "/rest" + PATH + "/" + id);
			id++;
			Log.info("createSheet : " + sheet + "\n");

			getUser(sheet.getOwner(), password, true);

			sheets.put(sheet.getSheetId(), sheet);
		}

		return sheet.getSheetId();
	}

	@Override
	public void deleteSpreadsheet(String sheetId, String password) {
		Log.info("deleteSheet : sheet = " + sheetId + "; pwd = " + password + "\n");

		if (sheetId == null) {
			Log.info("SheetId null.");
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		if (password == null) {
			Log.info("SheetId null.");
			throw new WebApplicationException(Status.FORBIDDEN);
		}
		Spreadsheet s;
		synchronized (this) {
			s = sheets.get(sheetId);

			if (s == null) {
				Log.info("SheetId null.");
				throw new WebApplicationException(Status.NOT_FOUND);
			}
		}

		getUser(s.getOwner(), password, false);

		synchronized (this) {
			sheets.remove(sheetId, s);
		}
	}

	@Override
	public Spreadsheet getSpreadsheet(String sheetId, String userId, String password) {
		Log.info("getSheet:sheet = " + sheetId + "; user = " + userId + "; pwd = " + password + "\n");

		if (sheetId == null) {
			Log.info("SheetId null erro 404.\n");
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		if (userId == null) {
			Log.info("userId null erro 404.\n");
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		if (password == null) {
			Log.info("password null erro 403.\n");
			throw new WebApplicationException(Status.FORBIDDEN);
		}

		Spreadsheet s;
		synchronized (this) {
			s = sheets.get(sheetId);
		}

		if (s == null) {
			Log.info("SheetId null erro 404.\n");
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		boolean isOwner = false;
		boolean isShared = false;
		synchronized (this) {
			isOwner = s.getOwner().equals(userId);
			isShared = s.getSharedWith().contains(userId + "@" + domain);
		}

		if (isOwner || isShared) {

			getUser(userId, password, false);

		} else {
			Log.info("user null erro 403.\n");
			throw new WebApplicationException(Status.FORBIDDEN);
		}

		return s;
	}

	@Override
	public String[][] getSpreadsheetValues(String sheetId, String userId, String password) {
		Log.info("getSheetValues : sheet = " + sheetId + "; user = " + userId + "; pwd = " + password + "\n");

		if (sheetId == null) {
			Log.info("SheetId null.");
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		if (userId == null) {
			Log.info("SheetId null.");
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		if (password == null) {
			Log.info("SheetId null.");
			throw new WebApplicationException(Status.FORBIDDEN);
		}

		Spreadsheet s;
		synchronized (this) {
			s = sheets.get(sheetId);
		}

		if (s == null) {
			Log.info("SheetId null.");
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		boolean isOwner = false;
		boolean isShared = false;
		synchronized (this) {
			isOwner = s.getOwner().equals(userId);
			isShared = s.getSharedWith().contains(userId + "@" + domain);
		}

		if (isOwner || isShared) {

			getUser(userId, password, false);

		} else {
			Log.info("user null.");
			throw new WebApplicationException(Status.FORBIDDEN);
		}

		return SpreadsheetEngineImpl.getInstance()
				.computeSpreadsheetValues(new AbstractSpreadSheetRestImpl(s, domain, cache, sheets, ip + ":8080", c));
	}

	@Override
	public void updateCell(String sheetId, String cell, String rawValue, String userId, String password) {
		Log.info("updateCell : sheet = " + sheetId + "; cell = " + cell + "; rawValue = " + rawValue + "; user = "
				+ userId + "; pwd = " + password + "\n");

		if (sheetId == null || cell == null || rawValue == null || userId == null) {
			Log.info("SheetId null.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		if (password == null) {
			Log.info("password null");
			throw new WebApplicationException(Status.FORBIDDEN);
		}

		Spreadsheet s;
		synchronized (this) {
			s = sheets.get(sheetId);
		}

		if (s == null) {
			Log.info("SheetId null.");
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		boolean isOwner = false;
		boolean isShared = false;
		synchronized (this) {
			isOwner = s.getOwner().equals(userId);
			isShared = s.getSharedWith().contains(userId + "@" + domain);
		}

		if (isOwner || isShared) {

			getUser(userId, password, false);

		} else {
			Log.info("user null.");
			throw new WebApplicationException(Status.FORBIDDEN);
		}

		synchronized (this) {
			s.setCellRawValue(cell, rawValue);
		}

	}

	@Override
	public void shareSpreadsheet(String sheetId, String userId, String password) {
		Log.info("shareSheet : sheet = " + sheetId + "; user = " + userId + "; pwd = " + password + "\n");

		if (sheetId == null) {
			Log.info("SheetId null.");
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		if (password == null) {
			Log.info("SheetId null.");
			throw new WebApplicationException(Status.FORBIDDEN);
		}

		Spreadsheet s;
		synchronized (this) {
			s = sheets.get(sheetId);
		}

		if (s == null) {
			Log.info("SheetId null.");
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		getUser(s.getOwner(), password, false);

		synchronized (this) {
			if (s.getSharedWith().contains(userId)) {
				Log.info("SheetId null.");
				throw new WebApplicationException(Status.CONFLICT);
			} else {

				s.getSharedWith().add(userId);
			}
		}

	}

	@Override
	public void unshareSpreadsheet(String sheetId, String userId, String password) {
		Log.info("unshareSheet : sheet = " + sheetId + "; user = " + userId + "; pwd = " + password + "\n");

		if (sheetId == null) {
			Log.info("SheetId null.");
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		if (userId == null) {
			Log.info("SheetId null.");
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		if (password == null) {
			Log.info("SheetId null.");
			throw new WebApplicationException(Status.FORBIDDEN);
		}

		Spreadsheet s;
		synchronized (this) {
			s = sheets.get(sheetId);
		}

		if (s == null) {
			Log.info("SheetId null.");
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		getUser(s.getOwner(), password, false);

		synchronized (this) {
			if (s.getSharedWith().contains(userId)) {

				s.getSharedWith().remove(userId);

			} else {
				throw new WebApplicationException(Status.BAD_REQUEST);
			}
		}

	}

	@Override
	public void deleteUserSpreadsheet(String userId, String secret) {
		if (secret.equals(this.secret)) {
			Log.info("deleting " + userId + " sheets\n");
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

	private void getUser(String userId, String password, boolean b) {
		URI serviceUri = d.knownUrisOf(domain + ":users");
		try {
			c.getUses(serviceUri, userId, password);
		} catch (Exception e) {
			if (b) {
				throw new WebApplicationException(Status.BAD_REQUEST);
			} else {
				throw new WebApplicationException(Status.FORBIDDEN);
			}
		}

	}

	@Override
	public String[][] getSpreadsheetImport(String sheetId, String userId, String secret) {
		Log.info("geting import = " + sheetId + "   userId  = " + userId);
		Spreadsheet s = sheets.get(sheetId);
		if (s.getSharedWith().contains(userId) && secret.equals(this.secret)) {
			Log.info("secret =  " + secret + "    fgfgfgdfgdfgdgfdfdfgfd\n");
			return SpreadsheetEngineImpl.getInstance()
					.computeSpreadsheetValues(new AbstractSpreadSheetRestImpl(s, domain, cache, sheets, ip + ":8080", c));
		}
		Log.info("no secret dfgfdfggfdbgdfbgfdfbgfd\n");
		return null;

	}

}
