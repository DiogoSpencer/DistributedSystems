package extras;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.namespace.QName;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import com.sun.xml.ws.client.BindingProviderProperties;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.WebServiceException;
import tp1.api.Spreadsheet;
import tp1.api.User;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.api.service.rest.RestSpreadsheetsReplication;
import tp1.api.service.rest.RestUsers;
import tp1.api.service.soap.SheetsException;
import tp1.api.service.soap.SoapSpreadsheets;
import tp1.api.service.soap.SoapUsers;
import tp1.api.service.soap.UsersException;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.gson.Gson;

public class Connection {

	private static Logger Log = Logger.getLogger(Connection.class.getName());

	private final static int MAX_RETRIES = 3;
	private final static int RETRY_PERIOD = 6000;
	private final static int CONNECTION_TIMEOUT = 1000;
	private final static int REPLY_TIMEOUT = 600;
	private Client client;
	private String secret;

	public Connection(String secret) {

		ClientConfig config = new ClientConfig();

		config.property(ClientProperties.CONNECT_TIMEOUT, 1000);
		config.property(ClientProperties.READ_TIMEOUT, 1000);
		client = ClientBuilder.newClient(config);

		this.secret = secret;

	}

	public User getUses(URI serviceUri, String userId, String password) throws UsersException {
		if (serviceUri.getPath().equals("/rest")) {
			return getUserRest(serviceUri, userId, password);
		} else {
			return getUserSoap(serviceUri, userId, password);
		}
	}

	private User getUserRest(URI serviceUri, String userId, String password) {

		User u = null;

		WebTarget target = client.target(serviceUri).path(RestUsers.PATH);

		boolean sucess = false;
		while (!sucess) {
			try {
				Response r = target.path(userId).queryParam("password", password).request()
						.accept(MediaType.APPLICATION_JSON).get();
				if (r.getStatus() == Status.OK.getStatusCode()) {
					u = r.readEntity(User.class);
					sucess = true;
				} else {

					throw new WebApplicationException(r.getStatus());
				}

			} catch (ProcessingException e) {
				e.printStackTrace();
				try {
					Thread.sleep(RETRY_PERIOD);
				} catch (InterruptedException i) {

				}
			}
		}
		return u;

	}

	private User getUserSoap(URI serviceUri, String userId, String password) throws UsersException {
		SoapUsers users = null;
		User u = null;

		try {
			QName QNAME = new QName(SoapUsers.NAMESPACE, SoapUsers.NAME);
			Service service = Service.create(new URL(serviceUri + "/users/?wsdl"), QNAME);
			users = service.getPort(tp1.api.service.soap.SoapUsers.class);
		} catch (WebServiceException | MalformedURLException e) {
			System.err.println("Could not contact the server: " + e.getMessage());
			System.exit(1);
		}

		// Set timeouts for executing operations
		((BindingProvider) users).getRequestContext().put(BindingProviderProperties.CONNECT_TIMEOUT, 1000);
		((BindingProvider) users).getRequestContext().put(BindingProviderProperties.REQUEST_TIMEOUT, 1000);

		System.out.println("Sending request to server.");

		boolean success = false;

		while (!success) {

			try {
				u = users.getUser(userId, password);
				success = true;
			} catch (WebServiceException wse) {
				System.out.println("Communication error.");
				wse.printStackTrace();
				try {
					Thread.sleep(RETRY_PERIOD);
				} catch (InterruptedException e) {
					// nothing to be done here, if this happens we will just retry sooner.
				}
				System.out.println("Retrying to execute request.");
			}
		}
		return u;
	}

	public void deleteSpread(URI serviceUri, String userId) {
		if (serviceUri.getPath().equals("/rest")) {
			deleteSpreadRest(serviceUri, userId);
		} else {
			deleteSpreadSoap(serviceUri, userId);
		}
	}

	private void deleteSpreadRest(URI serviceUri, String userId) {

		WebTarget target = client.target(serviceUri).path(RestSpreadsheets.PATH);

		boolean sucess = false;
		while (!sucess) {
			try {
				target.path(userId).path("delete").queryParam("secret", secret).request()
						.accept(MediaType.APPLICATION_JSON).delete();
				sucess = true;
			} catch (ProcessingException e) {
				e.printStackTrace();
				try {
					Thread.sleep(RETRY_PERIOD);
				} catch (InterruptedException i) {

				}
			}
		}

	}

	private void deleteSpreadSoap(URI serviceUri, String userId) {

		SoapSpreadsheets sheets = null;

		try {
			QName QNAME = new QName(SoapSpreadsheets.NAMESPACE, SoapSpreadsheets.NAME);
			Service service = Service.create(new URL(serviceUri + "/spreadsheets/?wsdl"), QNAME);
			sheets = service.getPort(SoapSpreadsheets.class);
		} catch (WebServiceException | MalformedURLException e) {
			System.exit(1);
		}

		// Set timeouts for executing operations
		((BindingProvider) sheets).getRequestContext().put(BindingProviderProperties.CONNECT_TIMEOUT, 1000);
		((BindingProvider) sheets).getRequestContext().put(BindingProviderProperties.REQUEST_TIMEOUT, 1000);

		System.out.println("Sending request to server.");

		boolean success = false;

		while (!success) {

			try {
				sheets.deleteUserSpreadsheet(userId, secret);
				;
				success = true;

			} catch (SheetsException e) {
				System.out.println("Cound not get user: " + e.getMessage());
				success = true;
			} catch (WebServiceException wse) {
				System.out.println("Communication error.");
				wse.printStackTrace();
				try {
					Thread.sleep(RETRY_PERIOD);
				} catch (InterruptedException e) {
					// nothing to be done here, if this happens we will just retry sooner.
				}
				System.out.println("Retrying to execute request.");
			}
		}

	}

	public String[][] importRange(String serviceUri, String userId) {
		Log.info("user1 :" + userId + "\n");
		String[] aux = serviceUri.split("/");
		if (aux[3].equals("rest")) {
			return importRest(serviceUri, userId);
		} else {
			return importSoap(userId, aux);
		}
	}

	private String[][] importRest(String serviceUri, String userId) {

		WebTarget target = client.target(serviceUri);

		short retries = 0;
		boolean sucess = false;
		while (!sucess && retries < MAX_RETRIES) {
			try {
				Response r = target.path("import").queryParam("userId", userId).queryParam("secret", secret).request()
						.accept(MediaType.APPLICATION_JSON).get();
				sucess = true;
				return r.readEntity(String[][].class);
			}

			catch (ProcessingException e) {
				e.printStackTrace();
				retries++;
				try {

					Thread.sleep(RETRY_PERIOD);
				} catch (InterruptedException i) {

				}
			}
		}
		return null;
	}

	private String[][] importSoap(String userId, String[] serviceUrl) {

		SoapSpreadsheets sheets = null;

		String[][] s = null;

		try {
			QName QNAME = new QName(SoapSpreadsheets.NAMESPACE, SoapSpreadsheets.NAME);
			Service service = Service.create(new URL(serviceUrl[0] + "//" + serviceUrl[2] + "/" + serviceUrl[3] + "/"
					+ serviceUrl[4] + "/" + serviceUrl[5]), QNAME);
			sheets = service.getPort(tp1.api.service.soap.SoapSpreadsheets.class);
		} catch (WebServiceException | MalformedURLException e) {

			System.exit(1);
		}

		// Set timeouts for executing operations
		((BindingProvider) sheets).getRequestContext().put(BindingProviderProperties.CONNECT_TIMEOUT,
				CONNECTION_TIMEOUT);
		((BindingProvider) sheets).getRequestContext().put(BindingProviderProperties.REQUEST_TIMEOUT, REPLY_TIMEOUT);

		System.out.println("Sending request to server.");

		short retries = 0;
		boolean success = false;

		while (!success && retries < MAX_RETRIES) {

			try {
				s = sheets.getImportValues(serviceUrl[6], userId, secret);
				;
				success = true;

			} catch (SheetsException e) {
				System.out.println("Cound not get user: " + e.getMessage());
				success = true;
			} catch (WebServiceException wse) {
				System.out.println("Communication error.");
				wse.printStackTrace();
				retries++;
				try {
					Thread.sleep(RETRY_PERIOD);
				} catch (InterruptedException e) {
					// nothing to be done here, if this happens we will just retry sooner.
				}
				System.out.println("Retrying to execute request.");
			}
		}
		return s;

	}

	public List<List<Object>> importRangeGoogle(String sheetURL) {

		WebTarget target = client.target(sheetURL);

		short retries = 0;
		boolean sucess = false;
		while (!sucess && retries < MAX_RETRIES) {
			try {
				Response r = target.request().get();
				sucess = true;
				String v = r.readEntity(String.class);
				Gson json = new Gson();
				return json.fromJson(v, ValueRange.class).getValues();

			}

			catch (ProcessingException e) {
				e.printStackTrace();
				retries++;
				try {

					Thread.sleep(RETRY_PERIOD);
				} catch (InterruptedException i) {

				}
			}
		}
		return null;
	}

	public void create(URI serviceUri, Spreadsheet sheet, Long version) {
		WebTarget target = client.target(serviceUri).path(RestSpreadsheetsReplication.PATH);

		short retries = 0;
		boolean sucess = false;
		while (!sucess && retries < MAX_RETRIES ) {
			try {
				Response r = target.path("serverCreate").queryParam("secret", secret).request()
						.header(RestSpreadsheetsReplication.HEADER_VERSION, version).accept(MediaType.APPLICATION_JSON)
						.post(Entity.entity(sheet, MediaType.APPLICATION_JSON));
				sucess = true;
			} catch (ProcessingException e) {
				e.printStackTrace();
				retries++;
				try {
					Thread.sleep(RETRY_PERIOD);
				} catch (InterruptedException i) {

				}
				
			}
		}
	}

	public void delete(URI serviceUri, String sheetId, Long version) {
		WebTarget target = client.target(serviceUri).path(RestSpreadsheetsReplication.PATH);

		short retries = 0;
		boolean sucess = false;
		while (!sucess && retries < MAX_RETRIES) {
			try {
				Response r = target.path("serverDelete").queryParam("sheetId", sheetId).queryParam("secret", secret)
						.request().header(RestSpreadsheetsReplication.HEADER_VERSION, version)
						.accept(MediaType.APPLICATION_JSON).delete();
				sucess = true;
			} catch (ProcessingException e) {
				e.printStackTrace();
				retries++;
				try {
					Thread.sleep(RETRY_PERIOD);
				} catch (InterruptedException i) {

				}
			}
		}
	}

	public void update(URI serviceUri, Spreadsheet sheet, Long version) {
		WebTarget target = client.target(serviceUri).path(RestSpreadsheetsReplication.PATH);

		short retries = 0;
		boolean sucess = false;
		while (!sucess && retries < MAX_RETRIES) {
			try {
				Response r = target.path("serverUpdate").queryParam("secret", secret).request()
						.header(RestSpreadsheetsReplication.HEADER_VERSION, version).accept(MediaType.APPLICATION_JSON)
						.put(Entity.entity(sheet, MediaType.APPLICATION_JSON));
				sucess = true;
				
			} catch (ProcessingException e) {
				e.printStackTrace();
				retries++;
				try {
					Thread.sleep(RETRY_PERIOD);
				} catch (InterruptedException i) {

				}
			}
		}
	}
	

	public void deleteUserSpreadsheetServer(URI serviceUri, String userId, Long version) {
		WebTarget target = client.target(serviceUri).path(RestSpreadsheetsReplication.PATH);

		short retries = 0;
		boolean sucess = false;
		while (!sucess && retries < MAX_RETRIES) {
		try {
			target.path("deleteUserSpreadsheetServer").queryParam("userId", userId).queryParam("secret", secret)
					.request().header(RestSpreadsheetsReplication.HEADER_VERSION, version)
					.accept(MediaType.APPLICATION_JSON).delete();
			sucess = true;

		} catch (ProcessingException e) {
			e.printStackTrace();
			retries++;
			try {
				Thread.sleep(RETRY_PERIOD);
			} catch (InterruptedException i) {

			}
		}}
	}

	public Update updateVersion(URI serviceUri) {
		WebTarget target = client.target(serviceUri).path(RestSpreadsheetsReplication.PATH);

		short retries = 0;
		boolean sucess = false;
		while (!sucess && retries < MAX_RETRIES) {
			try {
				Response r = target.path("updateVersion").queryParam("secret", secret).request()
						.accept(MediaType.APPLICATION_JSON).get();
				sucess = true;
				return r.readEntity(Update.class);
			} catch (ProcessingException e) {
				retries++;
				try {
					Thread.sleep(RETRY_PERIOD);
				} catch (InterruptedException i) {

				}
			}
		}
		return null;
	}

}
