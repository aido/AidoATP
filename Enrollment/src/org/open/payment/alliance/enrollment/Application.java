/**
 * 
 */
package org.open.payment.alliance.enrollment;

import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * @author Auberon
 *
 */
public class Application {

	private static HashMap<String,String> arguments;
	private static Logger log;
	private static ResourceBundle errors, info;
	private static ExecutorService pool;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Locale locale = Locale.getDefault();
		arguments = new HashMap<String,String>();
		parseArgs(args);
		log = Logger.getLogger(Application.class.getName());
		errors = ResourceBundle.getBundle("ErrorMessages", locale);
		info = ResourceBundle.getBundle("InfoMessages",locale);
		
		if(getArg("debugMode") != null && getArg("debugMode").equals("true")){
			daemonize();
		}
		
		EnrollmentServer server = EnrollmentServer.getInstance();
		pool = Executors.newCachedThreadPool();
		pool.submit(server);
		
	}
	private static void parseArgs(String[] args) {
		for(String arg : args){
			String[] argParts = arg.split("=");
			arguments.put(argParts[0].replace("-", ""), argParts[1]);
		}
	}
	
	public static String getArg(String key){
		return arguments.get(key);
	}
	public static String getError(String key) {
		return errors.getString(key);
	}
	
	public static String getInfo(String key) {
		return info.getString(key);
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
