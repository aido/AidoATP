package org.open.payment.alliance.enrollment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.logging.Logger;

import javax.net.ssl.SSLSocket;

public class ClientConnection implements Runnable {
	
	SSLSocket socket;
	private Logger log;
	public ClientConnection(SSLSocket socket){
		this.socket = socket;
	}

	@Override
	public void run() {
		try {
			InputStream in = socket.getInputStream();
			OutputStream out = socket.getOutputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			PrintWriter writer = new PrintWriter(out,true);
			log = Logger.getLogger(Application.class.getName());
			
			int lineCount;
			
			while(socket.isConnected()){
				lineCount = 0;
				String enrollmentID = null, userID = null;
				while(reader.ready()){
					if(lineCount == 0){
						enrollmentID = reader.readLine();
					}
					if(lineCount == 1){
						userID = reader.readLine();
					}
					if(lineCount == 2){
						EnrollmentModel model = EnrollmentManager.getInstance().enroll(enrollmentID.getBytes(), userID.getBytes());
						writer.println(model.getAddr().toString()+'\1');
						writer.println(model.getEiD().toString()+'\1');
						writer.println(model.getKey().toString()+'\1');
						
						break;
						
					}
					
					lineCount++;
				}
				socket.close();
				in.close();
				out.close();
			}
		} catch (IOException e) {
			log.severe(e.getLocalizedMessage());
		}
		System.gc();
	}

}
