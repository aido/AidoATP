package org.open.payment.alliance.enrollment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Arrays;

import javax.net.ssl.SSLSocket;

public class ClientConnection implements Runnable {
	
	private static int MAX_BUF_SIZE = 4096;
	private MessageQ enrollmentQ;
	SSLSocket socket;
	public ClientConnection(SSLSocket socket, MessageQ enrollmentQ){
		this.socket = socket;
		this.enrollmentQ = enrollmentQ;
	}

	@Override
	public void run() {
		try {
			InputStream in = socket.getInputStream();
			OutputStream out = socket.getOutputStream();
			InputStreamReader reader = new InputStreamReader(in);
			PrintWriter writer = new PrintWriter(out,true);
			byte[] enrollmentID = emptyArray(4096);
			byte[] userID = emptyArray(4096);
			
			int pos, lineCount;
			//
			while(socket.isConnected()){
				pos = lineCount = 0;
				while(reader.ready()){
					byte b = (byte) reader.read();
					if(b == '\1'){//Should be looking for ascii SOH character
						lineCount++;
						pos = 0;
					}
					if(b == '\3'){//Looking for ascii EOT character
						EnrollmentManager.getInstance().enroll(enrollmentID, userID, enrollmentQ);
					}
					if(lineCount == 0){
						enrollmentID[pos] = b;
					}
					if(lineCount == 1){
						userID[pos] = b;
					}
					if(lineCount > 1){
						//Protocol violation, just cut it off
						in.close();
						out.close();
						socket.close();
						Arrays.fill(enrollmentID, (byte)0);
						Arrays.fill(userID, (byte)0);
						return;
					}
					pos++;
					
				}
				break;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private byte[] emptyArray(int i) {
		byte[] arr = new byte[i];
		Arrays.fill(arr,(byte)0);
		return arr;
	}

}
