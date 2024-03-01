package sheetsDrop;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



import java.net.URI;
import java.security.SecureRandom;

import extras.Connection;
import extras.Discovery;
import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;
import sheetsDrop.util.CreateDirectory;
import sheetsDrop.util.CreateFile;
import sheetsDrop.util.DeleteFile;
import sheetsDrop.util.DownloadFile;
import tp1.api.Spreadsheet;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.impl.engine.AbstractSpreadsheetDropImpl;
import tp1.impl.engine.SpreadsheetEngineImpl;

@Singleton
public class sheetResourceDrop implements RestSpreadsheets {

	private final Map<String, String[][]> cache = new HashMap<String, String[][]>();
	private String domain;
	private Discovery d;
	private String ip;
	private Connection c;
	private String secret;

	private CreateFile cFile;
	private DownloadFile downF;
	private DeleteFile deleteF;

	private static Logger Log = Logger.getLogger(sheetResourceDrop.class.getName());

	public sheetResourceDrop(String domain, Discovery d, String ip, String secret, boolean delete) {
		cFile = new CreateFile();
		downF = new DownloadFile();
		deleteF = new DeleteFile();
		CreateDirectory directory = new CreateDirectory();

		if (delete) {
			deleteF.execute("/" + domain);
		}

		directory.execute("/" + domain);

		this.domain = domain;
		this.d = d;
		this.ip = ip;
		this.secret = secret;
		c = new Connection(secret);
	}

	@Override
	public String createSpreadsheet(Spreadsheet sheet, String password) {

		Log.info("createSheet : " + sheet + "\n");
		if (sheet.getOwner() == null || sheet.getRows() <= 0 || sheet.getColumns() <= 0) {
			Log.info("something bad.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}
		SecureRandom sR = new SecureRandom();
		synchronized (this) {
			String id = sR.nextInt() + "" + System.currentTimeMillis();
			sheet.setSheetId(id);
			sheet.setSheetURL("https://" + ip + ":" + 8080 + "/rest" + PATH + "/" + id);

			getUser(sheet.getOwner(), password, true);
			Log.info("ir buscar a lista do user\n");
			List<String> userSheets = downF.executeU(sheet.getOwner(), domain);
			
			if (userSheets != null) {
				userSheets.add(sheet.getSheetId());
			} else {
				userSheets = new LinkedList<String>();
				userSheets.add(sheet.getSheetId());
			}
			
			Log.info("meter a lista do user\n");
			cFile.execute(sheet.getOwner(), domain, userSheets);
			cFile.execute(id, domain, sheet);
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
			s = downF.execute(sheetId, domain);

			if (s == null) {
				Log.info("SheetId null.");
				throw new WebApplicationException(Status.NOT_FOUND);
			}
		}

		getUser(s.getOwner(), password, false);

		synchronized (this) {
			deleteF.execute("/" + domain + "/" + s.getSheetId());
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

		Spreadsheet s = null;
		synchronized (this) {
			
				s = downF.execute(sheetId, domain);
			
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
			s = downF.execute(sheetId, domain);
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
				.computeSpreadsheetValues(new AbstractSpreadsheetDropImpl(s, domain, cache, ip + ":8080", c));
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
			s = downF.execute(sheetId, domain);
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
			cFile.execute(s.getSheetId(), domain, s);

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
			s = downF.execute(sheetId, domain);
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
				cFile.execute(s.getSheetId(), domain, s);
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
			s = downF.execute(sheetId, domain);
		}

		if (s == null) {
			Log.info("SheetId null.");
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		getUser(s.getOwner(), password, false);

		synchronized (this) {
			if (s.getSharedWith().contains(userId)) {

				s.getSharedWith().remove(userId);
				cFile.execute(s.getSheetId(), domain, s);

			} else {
				throw new WebApplicationException(Status.BAD_REQUEST);
			}
		}

	}

	@Override
	public void deleteUserSpreadsheet(String userId, String secret) {
		Log.info("dfggfdsdfggdfgfdfgfd\n");
		Log.info("secretU     " + secret );
		Log.info("secretDrop     " + this.secret );
		if (secret.equals(this.secret)) {
			Log.info("deleting " + userId + " sheets\n");
			synchronized (this) {
				Log.info("ir buscar a lista do user\n");
				List<String> userSheets = downF.executeU(userId, domain);
				if(userSheets != null) {
					for (String sheetId : userSheets) {
						deleteF.execute("/" + domain + "/" + sheetId);
					}
					deleteF.execute("/" + domain + "/" + userId);
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
		Spreadsheet s = downF.execute(sheetId, domain);
		if (s.getSharedWith().contains(userId) && secret.equals(this.secret)) {
			return SpreadsheetEngineImpl.getInstance()
					.computeSpreadsheetValues(new AbstractSpreadsheetDropImpl(s, domain, cache, ip + ":8080", c));
		}

		return null;

	}

}
