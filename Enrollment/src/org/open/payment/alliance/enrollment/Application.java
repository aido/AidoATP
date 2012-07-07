/**
 * 
 */
package org.open.payment.alliance.enrollment;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

/**
 * @author Auberon
 *
 */
public class Application {

	private static HashMap<String,String> arguments;
	private static Logger log;
	//private static ResourceBundle errors, info;
	private static ExecutorService pool;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		arguments = new HashMap<String,String>();
		parseArgs(args);
		log = Logger.getLogger(Application.class.getName());
				
		if(getArg("debugMode") != null && getArg("debugMode").equals("true")){ //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			//daemonize();
		}
		
		EnrollmentServer server = EnrollmentServer.getInstance();
		pool = Executors.newCachedThreadPool();
		Future<?> end = pool.submit(server);
		try {
			while(end.get() != null){
				Thread.yield();
			}
		} catch (Exception e){
			log.severe(e.getLocalizedMessage());
		}
		
	}
	private static void parseArgs(String[] args) {
		for(String arg : args){
			String[] argParts = arg.split("="); //$NON-NLS-1$
			arguments.put(argParts[0].replace("-", ""), argParts[1]); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	public static String getArg(String key){
		return arguments.get(key);
	}

	static public void daemonize()
	{
	   System.out.close();
	   System.err.close();
	}
	
	public static ExecutorService getPool(){
		return pool;
	}
}
