package sheetsDrop;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import extras.Discovery;
import extras.InsecureHostnameVerifier;

public class SheetServerDrop {

	private static Logger Log = Logger.getLogger(SheetServerDrop.class.getName());

	static {
		System.setProperty("java.net.preferIPv4Stack", "true");
		System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
	}

	public static final int PORT = 8080;
	public static final String SERVICE = "SheetService";

	// change
	public static void main(String[] args) {
		try {

			String domain = args[0];
			boolean delete;

			if (args[1].equals("false")) {
				delete = false;
			} else {
				delete = true;
			}
			
			String secret = args[2];
			
			String ip = InetAddress.getLocalHost().getHostAddress();

			HttpsURLConnection.setDefaultHostnameVerifier(new InsecureHostnameVerifier());

			ResourceConfig config = new ResourceConfig();

			String serverURI = String.format("https://%s:%s/rest", ip, PORT);

			Discovery d = Discovery.getInstace();

			config.register(new sheetResourceDrop(domain, d, ip, secret, delete));

			JdkHttpServerFactory.createHttpServer(URI.create(serverURI), config, SSLContext.getDefault());

			d.Start(new InetSocketAddress("226.226.226.226", 2266), domain + ":" + "sheets", serverURI);

			Log.info(String.format("%s Server ready @ %s\n", SERVICE, serverURI));

			// More code can be executed here...
		} catch (Exception e) {
			Log.severe(e.getMessage());
		}
	}

}
