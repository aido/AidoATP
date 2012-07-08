/**
 * 
 */
package org.open.payment.alliance;

import java.util.ArrayList;
import java.util.HashMap;

import javax.crypto.spec.SecretKeySpec;

import org.jivesoftware.smack.packet.Message;

/**
 * @author Auberon
 *
 */
public class MessageProcessingService implements Runnable {

	private HashMap<String,Integer> transList; //Used to track transactions we've seen before
	private HashMap<String,SecretKeySpec> keyList; //Maps  MessageIDs to secret keys so we don't have to keep brute forcing them once we've seen them once.
	private HashMap<String, Long> expiryList; //Used to clean up transactions that have been sitting out too long
	private ArrayList<String>  ignoreList;//We've seen the MessageID on this message before, tried to decrypt it and it wasn't ours.  Probably not for us this time either.

	public MessageProcessingService(){
		transList = new HashMap<String,Integer>();
		keyList = new HashMap<String, SecretKeySpec>();
		expiryList = new HashMap<String,Long>();
		ignoreList = new ArrayList<String>();

	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		while(Application.getPlatform().isRunning()){
			ArrayList<Message> inbox = Application.getCommService().getInBox();
			for(Message msg : inbox){
				//Look at the MessageID
				
					//Is it on the ingoreList?
						//No?
						//Have we seen it before?
							//Yes?  Decrypt it and pass it through the process
							//No?   Attempt decryption
								//Did decryption work?
									//Yes? It's ours, move it through the process
									//No, add to ignoreList
						//Yes? Ignore
			}
		}
	}

}
