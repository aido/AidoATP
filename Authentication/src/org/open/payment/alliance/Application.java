package org.open.payment.alliance;

import org.open.payment.alliance.application.Platform;


public class Application {
	
	private static Platform platform;
	private static CommunicationService commService;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		platform = new Platform(args, Application.class.getName());
		if(platform.getArg("debugMode") != null && platform.getArg("debugMode").equals("true")){ 
			platform.daemonize();
		}
		
		CommunicationService commService = new CommunicationService();
		platform.submit(commService);
		
		MessageProcessingService mps = new MessageProcessingService();
		platform.submit(mps);
		
		while(platform.isRunning()){
			Thread.yield();
		}
	}
	
	public static Platform getPlatform(){
		return platform;
	}
	
	public static CommunicationService getCommService(){
		return commService;
	}
}
