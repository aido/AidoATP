package org.open.payment.alliance.messaging;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import org.jivesoftware.smack.packet.Message;

/**
 * Connects to the authentication and authorization service and passes the incoming messages as raw java objects
 * 
 * @author Auberon
 *
 */
public class AuthClientConnection implements Runnable{
	private Socket socket;
	private ObjectInputStream in;
	private ObjectOutputStream out;
	private ArrayList<Message> outbox;
	private ArrayList<Message> inbox;
	private Logger log;
	
	
	public AuthClientConnection(){
		try{
			log = Logger.getLogger(AuthClientConnection.class.getName());
			outbox = new ArrayList<Message>();
			inbox = new ArrayList<Message>();
			String authServer = Application.getPlatform().getPref("authserver");
			Integer authPort = Integer.parseInt(Application.getPlatform().getPref("authPort"));
			SocketFactory socketFactory = SSLSocketFactory.getDefault();
		    socket = socketFactory.createSocket(authServer, authPort);

		    // Create streams to securely send and receive data to the server
		    in = new ObjectInputStream(socket.getInputStream());
		    out = new ObjectOutputStream(socket.getOutputStream());
		}catch (Exception e){
			log.severe(e.getLocalizedMessage());
			Application.getPlatform().shutDown();
		}
	}
	
	@Override
	public void run() {
		while(Application.getPlatform().isRunning()){
			synchronized(outbox){
				for(Message msg : outbox){
					try {
						out.writeObject(msg);
					} catch (IOException e) {
						log.severe(e.getLocalizedMessage());
					}
				}
				outbox.clear();
			}
			synchronized(inbox){
				try {
					while(in.available() > 0){
						inbox.add((Message) in.readObject());
					}
				} catch (Exception e) {
					log.severe(e.getLocalizedMessage());
				}
			}
		}
	}
	
	public ArrayList<Message> getMsgs(){
		ArrayList<Message> ret;
		synchronized(inbox){
			ret = new ArrayList<Message>(inbox);
			inbox.clear();
		}
		return ret;
	}
	
	public void addMsg(Message msg){
		synchronized(outbox){
			outbox.add(msg);
		}
	}
	
	
}
