package org.open.payment.alliance.messaging;

import java.util.Collection;
import java.util.Date;
import java.util.logging.Logger;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.packet.Message;

public class OpenPayMessageListener implements MessageListener{
	private static final int TTL = 5 * 60 * 1000;
	private Connection conn;
	private ChatManager manager;
	private static Logger log;
	public OpenPayMessageListener(Connection conn,ChatManager manager){
		this.conn = conn;
		this.manager = manager;
	}
	@Override
	public void processMessage(Chat chat, Message msg) {
		log = Logger.getLogger(OpenPayMessageListener.class.getName());
		Long timestamp = Long.parseLong(msg.getSubject());
		Date now = new Date();
		if((now.getTime() - timestamp) < TTL){
			forwardToAll(msg);
			Application.sendMessageToAuth(msg);
		}
		
	}
	
	
	/**
	 * The secret to onion routing.
	 * Forward to everyone on our roster except the one who sent it to us.
	 * @param msg
	 */
	private void forwardToAll(Message msg) {
		Roster roster = conn.getRoster();
		String source = msg.getFrom();
		msg.setFrom(conn.getUser());
		Collection<RosterEntry> entries = roster.getEntries();
		for (RosterEntry entry : entries) {
			if(!entry.getUser().equals(source)){
			    if(roster.getPresence(entry.getUser()) != null){
			    	Chat newChat = manager.createChat(entry.getUser(), new OpenPayMessageListener(conn,manager));
			    	try {
						newChat.sendMessage(msg);
					} catch (Exception e) {
						log.severe(e.getLocalizedMessage());
					}
			    }
			}
		}
	}
	

}
