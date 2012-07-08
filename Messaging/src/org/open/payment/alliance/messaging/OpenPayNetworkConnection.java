/**
 * 
 */
package org.open.payment.alliance.messaging;

import java.util.logging.Logger;

import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.open.payment.alliance.application.Platform;

/**
 * @author Auberon
 *
 */
public class OpenPayNetworkConnection implements Runnable {
	
	Platform platform;
	ConnectionConfiguration config;
	Connection connection;
	String userName, password, xmppServer;
	Integer port;
	Logger log;
	ChatManager chatManager;
	
	/**
	 * 
	 */
	public OpenPayNetworkConnection() {
		platform = Application.getPlatform();
		log = Logger.getLogger(OpenPayNetworkConnection.class.getName());
		userName = platform.getPref("xmppname");
		password = platform.getPref("xmpppassword");
		port = Integer.parseInt(platform.getPref("xmppport"));
		xmppServer = platform.getPref("xmppserver");
		
		// Create the configuration for this new connection
		config = new ConnectionConfiguration(xmppServer, port);
		config.setCompressionEnabled(true);
		config.setSASLAuthenticationEnabled(true);
		connection = new XMPPConnection(config);
		
		// Connect to the server
		try {
			// Log into the server
			connection.connect();
			connection.login(userName, password);
			chatManager = connection.getChatManager();
			chatManager.addChatListener(new OpenPayChatManagerListener(connection,chatManager));

		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
		}
		
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		
		

		while(platform.isRunning()){
			
			
			Thread.yield();
		}
		// Disconnect from the server
		connection.disconnect();

	}

}
