package org.open.payment.alliance;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Logger;

import org.jivesoftware.smack.packet.Message;

public class ConnectionHandler implements Runnable {

	private Socket socket;
	private Logger log;
	private CommunicationService service;
	public ConnectionHandler(Socket socket, CommunicationService service){
		log = Logger.getLogger(ConnectionHandler.class.getName());
		this.socket = socket;
		this.service = service;
	}
	@Override
	public void run() {
		try {
			ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
			ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
			while(Application.getPlatform().isRunning() && socket.isConnected()){
				readIncoming(in);
				sendOutGoing(out);
				Thread.yield();
			}
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			Application.getPlatform().shutDown();
		}
	}
	
	private void sendOutGoing(ObjectOutputStream out) throws IOException {
		ArrayList<Message> outbox = service.getOutBox();
		for(Message msg: outbox){	
			out.writeObject(msg);	
		}
		
	}
	
	private void readIncoming(ObjectInputStream in) throws IOException, ClassNotFoundException {
		while(in.available() > 0){
			service.addToInBox((Message) in.readObject());
		}
	}

}
