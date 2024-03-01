package sheetsRestReplication;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.URI;

import extras.Cache;
import extras.Connection;
import extras.Discovery;
import extras.SyncPoint;
import extras.Update;
import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import tp1.api.Spreadsheet;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.api.service.rest.RestSpreadsheetsReplication;
import tp1.impl.engine.AbstractSpreadSheetImpl;
import tp1.impl.engine.AbstractSpreadSheetRestImpl;
import tp1.impl.engine.SpreadsheetEngineImpl;

@Singleton
public class sheetResource implements RestSpreadsheetsReplication {

	private Map<String, Spreadsheet> sheets = new HashMap<String, Spreadsheet>();
	private final Map<String, Cache> cache = new HashMap<String, Cache>();
	private String domain;
	private Discovery d;
	private int id = 0;
	private String ip;
	private Connection c;
	private String secret;
	private boolean primary;
	private long v;
	private SyncPoint sp;

	private static Logger Log = Logger.getLogger(sheetResource.class.getName());

	public sheetResource(String domain, Discovery d, String ip, String secret, boolean primary) {
		this.domain = domain;
		this.d = d;
		this.ip = ip;
		this.secret = secret;
		c = new Connection(secret);
		this.primary = primary;
		v = 0;
		sp = SyncPoint.getInstance();
		sp.setResult(v);
	}

	@Override
	public String createSpreadsheet(Long version, Spreadsheet sheet, String password) {

		if (!primary) {
			Log.info("redirecting to primary from " + ip + "\n");
			URI Uri = d.knownUrisOf(domain + ":sheetsP");
			throw new WebApplicationException(Response
					.temporaryRedirect(URI.create(Uri + RestSpreadsheetsReplication.PATH + "?password=" + password))
					.build());
		}

		if (sheet.getOwner() == null || sheet.getRows() <= 0 || sheet.getColumns() <= 0) {
			Log.info("something bad.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}
		synchronized (this) {
			sheet.setSheetId("" + id);
			sheet.setSheetURL("https://" + ip + ":" + 8080 + "/rest" + PATH + "/" + id);
			Log.info("createSheet : " + id + "\n");
			id++;

			Log.info("password   " + password);

			getUser(sheet.getOwner(), password, true);

			sheets.put(sheet.getSheetId(), sheet);
			v++;
		}
		
		List<String> Uri = d.knownUrisOfL(domain + ":sheetsS");
		
		for (String string : Uri) {
			new Thread(() -> {
				c.create(URI.create(string), sheet, v);
				sp.setResult(v);	
			
		}).start();
		}
		
		sp.waitForVersion(v);
		
		
		
			throw new WebApplicationException(Response.status(200).header(RestSpreadsheetsReplication.HEADER_VERSION, v)
				.entity(sheet.getSheetId()).build());
		
		
		

		

	}

	@Override
	public void deleteSpreadsheet(Long version, String sheetId, String password) {
		Log.info("deleteSheet : sheet = " + sheetId + "; pwd = " + password + "\n");

		if (!primary) {
			URI Uri = d.knownUrisOf(domain + ":sheetsP");
			throw new WebApplicationException(Response
					.temporaryRedirect(URI
							.create(Uri + RestSpreadsheetsReplication.PATH + "/" + sheetId + "?password=" + password))
					.build());
		}

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
			v++;
		}
		
		List<String> Uri = d.knownUrisOfL(domain + ":sheetsS");
		
		for (String string : Uri) {
			new Thread(() -> {
				c.delete(URI.create(string), sheetId, v);
				sp.setResult(v);	
			
		}).start();
		}
		
		sp.waitForVersion(v);
	}

	@Override
	public Spreadsheet getSpreadsheet(Long version, String sheetId, String userId, String password) {

		if (version > v) {
			URI Uri = d.knownUrisOf(domain + ":sheetsP");
			Update u = c.updateVersion(Uri);
			
			Log.info("a atualizar o servidor replicado");
			sheets = u.getSheets();
			v = u.getVersion();
			sp.setResult(v);
		}
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
	public String[][] getSpreadsheetValues(Long version, String sheetId, String userId, String password) {
		if (version > v) {
			URI Uri = d.knownUrisOf(domain + ":sheetsP");
			Update u = c.updateVersion(Uri);
			sheets = u.getSheets();
			v = u.getVersion();
			sp.setResult(v);
		}
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
	public void updateCell(Long version, String sheetId, String cell, String rawValue, String userId, String password) {
		if (!primary) {
			URI Uri = d.knownUrisOf(domain + ":sheetsP");
			throw new WebApplicationException(
					Response.temporaryRedirect(URI.create(Uri + RestSpreadsheetsReplication.PATH + "/" + sheetId + "/"
							+ cell + "?userId=" + userId + "&password=" + password)).build());
		}

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
			v++;
		}

List<String> Uri = d.knownUrisOfL(domain + ":sheetsS");
		
		for (String string : Uri) {
			new Thread(() -> {
				c.update(URI.create(string), s, v);
				sp.setResult(v);	
			
		}).start();
		}
		
		sp.waitForVersion(v);

	}

	@Override
	public void shareSpreadsheet(Long version, String sheetId, String userId, String password) {

		if (!primary) {
			URI Uri = d.knownUrisOf(domain + ":sheetsP");
			throw new WebApplicationException(
					Response.temporaryRedirect(URI.create(Uri + RestSpreadsheetsReplication.PATH + "/" + sheetId
							+ "/share/" + userId + "?password=" + password)).build());
		}
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
				v++;
			}
		}

List<String> Uri = d.knownUrisOfL(domain + ":sheetsS");
		
		for (String string : Uri) {
			new Thread(() -> {
				c.update(URI.create(string), s, v);
				sp.setResult(v);	
			
		}).start();
		}
		
		sp.waitForVersion(v);

	}

	@Override
	public void unshareSpreadsheet(Long version, String sheetId, String userId, String password) {

		if (!primary) {
			URI Uri = d.knownUrisOf(domain + ":sheetsP");
			throw new WebApplicationException(
					Response.temporaryRedirect(URI.create(Uri + RestSpreadsheetsReplication.PATH + "/" + sheetId
							+ "/share/" + userId + "?password=" + password)).build());
		}
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
				v++;

			} else {
				throw new WebApplicationException(Status.BAD_REQUEST);
			}
		}

List<String> Uri = d.knownUrisOfL(domain + ":sheetsS");
		
		for (String string : Uri) {
			new Thread(() -> {
				c.update(URI.create(string), s, v);
				sp.setResult(v);	
			
		}).start();
		}
		
		sp.waitForVersion(v);

	}

	@Override
	public void deleteUserSpreadsheet(String userId, String secret) {
		if (!primary) {
			URI Uri = d.knownUrisOf(domain + ":sheetsP");
			throw new WebApplicationException(Response.temporaryRedirect(Uri).build());
		}
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
				v++;
			}
			List<String> Uri = d.knownUrisOfL(domain + ":sheetsS");

			for (String u : Uri) {
				c.deleteUserSpreadsheetServer(URI.create(u), userId, v);
				;
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
			return SpreadsheetEngineImpl.getInstance().computeSpreadsheetValues(
					new AbstractSpreadSheetRestImpl(s, domain, cache, sheets, ip + ":8080", c));
		}
		return null;

	}

	@Override
	public void createSpreadsheetServer(Long version, Spreadsheet sheet, String secret) {
		if(version != v +1 && version > v) {
				sp.waitForVersion(version);
			
		}
		
		if (this.secret.equals(secret) && version == v+1) {
			Log.info("creating spreadsheet Replication");

			v = version;
			sheets.put(sheet.getSheetId(), sheet);
			sp.setResult(v);
		}

	}

	@Override
	public void deleteSpreadsheetServer(Long version, String sheetId, String secret) {
		if(version != v +1) {
			sp.waitForVersion(version);
		}
		if (this.secret.equals(secret) && version == v+1) {
			Log.info("deleting spreadsheet Replication");
			v = version;
			sheets.remove(sheetId);
			sp.setResult(v);
		}

	}

	@Override
	public void updateCellServer(Long version, Spreadsheet sheet, String password) {
		if(version != v +1) {
			sp.waitForVersion(version);
		}
		if (this.secret.equals(secret) && version == v+1) {
			Log.info("update spreadsheet Replication");
			v = version;
			sheets.replace(sheet.getSheetId(), sheet);
			sp.setResult(v);
		}

	}

	@Override
	public void deleteUserSpreadsheetServer(Long version, String userId, String secret) {
		if(version != v +1) {
			sp.waitForVersion(version);
		}
		if (secret.equals(this.secret) && version == v+1) {
			v = version;
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
			sp.setResult(v);
		}

	}

	@Override
	public Update update(String secret) {
		if (secret.equals(this.secret)) {
			return new Update(sheets, v);
		}
		return null;
	}

}
