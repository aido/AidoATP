/**
 * 
 */
package org.open.payment.alliance.messaging;


import org.jivesoftware.smack.packet.Message;
import org.open.payment.alliance.application.Platform;
/**
 * This is the entrance point for the messaging application.
 * The messaging service does two things.
 * It forwards all messages received to all contacts on the roster so long as the TTL for the message is not expired.
 * It forwards all messages received to the authentication service(s) using a secured channel.
 * 
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

	public static void sendMessageToAuth(Message msg) {
		// TODO Auto-generated method stub
		
	}
}
