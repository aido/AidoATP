/**
 * 
 */
package org.open.payment.alliance.messaging;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.open.payment.alliance.application.Platform;

/**
 * @author Auberon
 *
 */
public class OpenPayNetworkConnection implements Runnable {
	
	Platform platform;
	ConnectionConfiguration config;
	Connection connection;
	String userName, password, resourceName;
	Integer port;
	
	/**
	 * 
	 */
	public OpenPayNetworkConnection() {
		platform = Application.getPlatform();
		
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		
		// Create the configuration for this new connection
		config = new ConnectionConfiguration("jabber.org", 5222);
		config.setCompressionEnabled(true);
		config.setSASLAuthenticationEnabled(true);
		connection = new XMPPConnection(config);
		// Connect to the server
		try {
			connection.connect();
			connection.login(userName, password, resourceName);
		} catch (XMPPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Log into the server
		
		while(platform.isRunning()){
			
			
			Thread.yield();
		}

	}

}
