/**
 * 
 */
package org.open.payment.alliance.enrollment;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

/**
 * @author Auberon
 *
 */
public class EnrollmentServer implements Runnable {
	private static EnrollmentServer instance= null;
	private Integer port,backlog, threadCount;
	private SSLServerSocket serverSocket;
	private ExecutorService pool;
	private static Logger log;
	
	public EnrollmentServer(){
		
		threadCount = 10;
		log = Logger.getLogger(EnrollmentServer.class.getName());
		
		if(Application.getArg("port")!= null){
			port =Integer.parseInt(Application.getArg("port"));
		}
		log.info(Application.getInfo("PORT_BIND")+" "+port);
		
		if(Application.getArg("backlog")!=null){
			backlog = Integer.parseInt(Application.getArg("backlog"));
		}
		
		if(Application.getArg("minthreads") !=null){
			threadCount = Integer.parseInt(Application.getArg("minthreads"));
		}
		
		
		
		try {
			SSLServerSocketFactory socketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
			
			serverSocket = (SSLServerSocket) socketFactory.createServerSocket(port);
			serverSocket.setReuseAddress(true);
			
		} catch (IOException e) {
			log.log(Level.SEVERE, Application.getError("PORT_BIND_ERROR"));
			log.log(Level.SEVERE,e.getLocalizedMessage());
		}
	}
	
	public static EnrollmentServer getInstance(){
		if(instance == null){
			instance = new EnrollmentServer();
		}
		return instance;
	}

	@Override
	public void run() {
		while(true){
			SSLSocket clientSocket = null;
			try {
			    clientSocket = (SSLSocket) serverSocket.accept();
			  //TODO: Need to implement MessageQ and load it dynamically
			    Application.getPool().submit(new ClientConnection(clientSocket,null));
			}catch (IOException e) {
			    log.log(Level.SEVERE,Application.getError("COULD_NOT_START_SERVICE"));
			}
		}
	}
}
