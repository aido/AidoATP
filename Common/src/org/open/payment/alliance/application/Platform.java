/**
 * 
 */
package org.open.payment.alliance.application;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * @author Auberon
 * Class to handle common application tasks such as deamonizing, threading, parsing arguments etc
 */
public class Platform {

	private static HashMap<String,String> arguments;
	//private static Logger log;
	private boolean running;
	private SecurePreferences prefs;
	private static ExecutorService pool;
	/**
	 * @param args
	 */
	public Platform(String[] args,String className) {
		
		arguments = new HashMap<String,String>();
		parseArgs(args);
		//log = Logger.getLogger(className);
		prefs = new SecurePreferences();
		if(!getArg("clearprefs").isEmpty()){
			prefs.wipe();
			prefs = new SecurePreferences();
		}
		pool = Executors.newCachedThreadPool();
		running = true;
		
				
	}
	private void parseArgs(String[] args) {
		for(String arg : args){
			String[] argParts = arg.split("=");
			arguments.put(argParts[0].replace("-", ""), argParts[1]);
		}
	}
	
	public String getArg(String key){
		return arguments.get(key);
	}

	public void daemonize()
	{
	   System.out.close();
	   System.err.close();
	}
	
	public ExecutorService getPool(){
		return pool;
	}
	public void submit(Runnable obj) {
		pool.submit(obj);
	}
	
	public boolean isRunning(){
		return running;
	}
	
	public void shutDown(){
		//When we start again we want to run with the same args, we just don't want them to be obvious in ps -aux or command history etc.
		if(!getArg("clearprefs").isEmpty()){
			arguments.remove("clearprefs");
		}
		for(String key : arguments.keySet()){
			prefs.put(key, arguments.get(key));
		}
		running = false;
	}
}
