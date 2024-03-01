package sheetsSoap;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

import extras.Discovery;
import extras.InsecureHostnameVerifier;
import jakarta.xml.ws.Endpoint;

public class SheetsServer {

	private static Logger Log = Logger.getLogger(SheetsServer.class.getName());

	static {
		System.setProperty("java.net.preferIPv4Stack", "true");
		System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
	}

	public static final int PORT = 8080;
	public static final String SERVICE = "SpreadsheetsService";
	public static final String SOAP_SPREADSHEETS_PATH = "/soap/spreadsheets";

	//change
	public static void main(String[] args) {
		try {
			String domain = args[0];
			String secret = args[1];
			String ip = InetAddress.getLocalHost().getHostAddress();
			
			HttpsURLConnection.setDefaultHostnameVerifier(new InsecureHostnameVerifier());

			String serverURI = String.format("https://%s:%s/soap", ip, PORT);

			HttpsConfigurator config = new HttpsConfigurator(SSLContext.getDefault());

			Discovery d = Discovery.getInstace();

			HttpsServer server = HttpsServer.create(new InetSocketAddress(ip, PORT), 0);

			server.setHttpsConfigurator(config);

			server.setExecutor(Executors.newCachedThreadPool());

			Endpoint soapRestEndpoint = Endpoint.create(new SheetsWS(domain, d, ip, secret));

			soapRestEndpoint.publish(server.createContext(SOAP_SPREADSHEETS_PATH));

			server.start();

			d.Start(new InetSocketAddress("226.226.226.226", 2266), domain + ":" + "sheets", serverURI);

			Log.info(String.format("%s Server ready @ %s\n", SERVICE, serverURI));

			// More code can be executed here...
		} catch (Exception e) {
			Log.severe(e.getMessage());
		}
	}

}
