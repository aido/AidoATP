/**
 * 
 */
package org.open.payment.alliance.messaging;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
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
	Roster roster;
	
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
			roster = connection.getRoster();
			chatManager = connection.getChatManager();
			chatManager.addChatListener(new OpenPayChatManagerListener(connection,chatManager));
			

		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			Application.getPlatform().shutDown();
		}
		
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		while(platform.isRunning()){
			ArrayList<Message> outbox = Application.getAuthConnection().getMsgs();
			for(Message msg : outbox){
				msg.setFrom(connection.getUser());
				String timestamp = String.valueOf(System.currentTimeMillis());
				msg.setSubject(timestamp);
				Collection<RosterEntry> entries = roster.getEntries();
				for (RosterEntry entry : entries) {
					if(roster.getPresence(entry.getUser()) != null){
						Chat chat = chatManager.createChat(entry.getUser(), new OpenPayMessageListener(connection,chatManager));
						try {
							chat.sendMessage(msg);
						} catch (Exception e) {
							log.severe(e.getLocalizedMessage());
						}
					}
				}
			}
			Thread.yield();
		}
		// Disconnect from the server
		connection.disconnect();

	}

}
