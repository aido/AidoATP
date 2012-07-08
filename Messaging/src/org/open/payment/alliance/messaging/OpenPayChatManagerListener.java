package org.open.payment.alliance.messaging;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.Connection;

public class OpenPayChatManagerListener implements ChatManagerListener {
	private Connection conn;
	private ChatManager manager;
	
	public OpenPayChatManagerListener(Connection conn, ChatManager manager){
		this.conn = conn;
		this.manager = manager;
	}

	@Override
	public void chatCreated(Chat chat, boolean createdLocally) {
		if (!createdLocally){
            chat.addMessageListener(new OpenPayMessageListener(conn, manager));
		}
	}

}
