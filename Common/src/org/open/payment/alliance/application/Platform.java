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
		}else{
			for(String key : arguments.keySet()){
				prefs.put(key, arguments.get(key));
			}
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
	
	public String getPref(String key){
		return prefs.get(key);
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
		
		running = false;
	}
}
