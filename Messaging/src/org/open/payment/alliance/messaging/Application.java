/**
 * 
 */
package org.open.payment.alliance.messaging;


import org.open.payment.alliance.application.Platform;
/**
 * @author Auberon
 *
 */
public class Application{

	private static Platform platform = null;
	 
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		platform = new Platform(args, Application.class.getName());
		if(platform.getArg("debugMode") != null && platform.getArg("debugMode").equals("true")){ 
			platform.daemonize();
		}
		
		platform.submit(new OpenPayNetworkConnection());
	}

	public static Platform getPlatform(){
		return platform;
	}
}
