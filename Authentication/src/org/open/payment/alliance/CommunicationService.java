/**
 * 
 */
package org.open.payment.alliance;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.jivesoftware.smack.packet.Message;

/**
 * @author Auberon
 *
 */
public class CommunicationService implements Runnable {
	
	
	private ArrayList<Message> inbox;
	private ArrayList<Message> outbox;
	private ServerSocket socket;
	private ObjectOutputStream out;
	private ObjectInputStream in;
	
	private Logger log;
	
	public CommunicationService(){
		log = Logger.getLogger(CommunicationService.class.getName());
		inbox = new ArrayList<Message>();
		outbox = new ArrayList<Message>();
		try{
			Integer port = Integer.parseInt(Application.getPlatform().getPref("listenport"));
			Integer backLog = Integer.parseInt(Application.getPlatform().getPref("maxbacklog"));
			socket = SSLServerSocketFactory.getDefault().createServerSocket(port, backLog);
		}catch(Exception ex){
			log.severe(ex.getLocalizedMessage());
			Application.getPlatform().shutDown();
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		while(Application.getPlatform().isRunning()){
			
			try {
				Socket newSock = socket.accept();
				Application.getPlatform().submit(new ConnectionHandler(newSock,this));			
				
			} catch (Exception e) {
				log.severe(e.getLocalizedMessage());
				Application.getPlatform().shutDown();
			}
			//Read basic info
			
			//If it's not on the ignorelist then add it to the inbox
			
			Thread.yield();
		}
	}
	
	public ArrayList<Message> getInBox(){
		ArrayList<Message> ret;
		synchronized(inbox){
			ret = new ArrayList<Message>(inbox);
			inbox.clear();
		}
		return ret;
	}
	
	public ArrayList<Message> getOutBox() {
		ArrayList<Message> ret;
		synchronized(outbox){
			ret = new ArrayList<Message>(outbox);
			outbox.clear();
		}
		return ret;
	}
	
	public void addToOutBox(Message msg){
		synchronized(outbox){
			outbox.add(msg);
		}
	}

	public void addToInBox(Message msg) {
		synchronized(inbox){
			inbox.add(msg);
		}
	}

	

}
