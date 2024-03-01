package extras;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import sheetsRestReplication.sheetResource;

public class SyncPoint
{
	
	private static Logger Log = Logger.getLogger(SyncPoint.class.getName());
	private static SyncPoint instance;
	public static SyncPoint getInstance() {
		if( instance == null)
			instance = new SyncPoint();
		return instance;
	}

	private long version;
	
	
	private SyncPoint() {
	}
	
	/**
	 * Waits for version to be at least equals to n
	 */
	public synchronized void waitForVersion( long n) { 
		Log.info("wait oh filho \n");
		while( version < n) {
			try {
				wait();
			} catch (InterruptedException e) {
				// do nothing
			}
		}
	}

	public synchronized void setResult( long n) {
		version = n;
		notifyAll();
	}

}
