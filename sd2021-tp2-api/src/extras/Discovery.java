package extras;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;


public class Discovery {
	private static Logger Log = Logger.getLogger(Discovery.class.getName());
	
	static {
		// addresses some multicast issues on some TCP/IP stacks
		System.setProperty("java.net.preferIPv4Stack", "true");
		// summarizes the logging format
		System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s");
	}
	
	static final int DISCOVERY_PERIOD = 1000;
	static final int DISCOVERY_TIMEOUT = 5000;
	static final int CAPACITY = 5000;

	// Used separate the two fields that make up a service announcement.
	private static final String DELIMITER = "\t";
	private Map<String, Set<String>> map;
	private static Discovery instace;
	
	public static Discovery getInstace() {
		if(instace == null) {
			return new Discovery();
		}else {
			return instace;
		}
	}
	
	public Discovery() {
		
	}
	
	public void Start(InetSocketAddress addr, String serviceName, String serviceURI) {
		map = new HashMap<String, Set<String>>(10);
		
Log.info(String.format("Starting Discovery announcements on: %s for: %s -> %s", addr, serviceName, serviceURI));
		
		byte[] announceBytes = String.format("%s%s%s", serviceName, DELIMITER, serviceURI).getBytes();
		DatagramPacket announcePkt = new DatagramPacket(announceBytes, announceBytes.length, addr);

		try {
			MulticastSocket ms = new MulticastSocket( addr.getPort());
			ms.joinGroup(addr, NetworkInterface.getByInetAddress(InetAddress.getLocalHost()));
			// start thread to send periodic announcements
			new Thread(() -> {
				for (;;) {
					try {
						ms.send(announcePkt);
						Thread.sleep(DISCOVERY_PERIOD);
					} catch (Exception e) {
						e.printStackTrace();
						// do nothing
					}
				}
			}).start();
			
			// start thread to collect announcements
			new Thread(() -> {
				DatagramPacket pkt = new DatagramPacket(new byte[1024], 1024);
				for (;;) {
					try {
						pkt.setLength(1024);
						ms.receive(pkt);
						String msg = new String( pkt.getData(), 0, pkt.getLength());
						String[] msgElems = msg.split(DELIMITER);
						if( msgElems.length == 2) {	//periodic announcement
							Log.info(String.format( "FROM %s (%s) : %s\n", pkt.getAddress().getCanonicalHostName(), 
									pkt.getAddress().getHostAddress(), msg));
							//TODO: to complete by recording the received information from the other node.
							if(!map.containsKey(msgElems[0])) {
								Set<String> s = new HashSet<String>();
								s.add(msgElems[1]);
								map.put(msgElems[0], s);
							}else {
								Set<String> s = map.get(msgElems[0]);
								s.add(msgElems[1]);
								map.replace(msgElems[0], s);
							}
						}
					} catch (IOException e) {
						// do nothing
					}
				}
			}).start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public URI knownUrisOf(String serviceName) {
		if(map.containsKey(serviceName)) {
			Iterator<String> i = map.get(serviceName).iterator();
			String s = null;
			while (i.hasNext()) {
				s = i.next();
				
			}
			
			
			return URI.create(s);
		}else {
			throw new Error("nao ta aqui");
		}
	}
	
	public List<String> knownUrisOfL(String serviceName) {
		if(map.containsKey(serviceName)) {
			Iterator<String> i = map.get(serviceName).iterator();
			List<String> s = new LinkedList<String>();
			String aux = i.next();
			s.add(aux);
			while (i.hasNext()) {
				aux = i.next();
				for (String string : s) {
					if(!string.equals(aux)) {
						break;
					}
				}
				s.add(aux);
			}
			
			return s;
		}else {
			throw new Error("nao ta aqui");
		}
	}

}
