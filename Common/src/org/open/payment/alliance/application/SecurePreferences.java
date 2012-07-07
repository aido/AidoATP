/**
 * 
 */
package org.open.payment.alliance.application;


import java.net.NetworkInterface;
import java.security.MessageDigest;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;



/**
 * @author Auberon
 * This class is intended as a naive and simple wrapper around a preferences object.
 * It's primary purpose it to provide a secure, persistent, key/value store.
 */
public class SecurePreferences {
	private static Logger log;
	private Preferences prefs;
	private Cipher cipher;
	private SecretKeySpec secretKeySpec;
	
	public SecurePreferences(){
		log = Logger.getLogger(SecurePreferences.class.getName());
		
		try {
			prefs = Preferences.userRoot();
			NetworkInterface ni = NetworkInterface.getNetworkInterfaces().nextElement();
			MessageDigest sha = MessageDigest.getInstance("SHA-256");
			byte[] key = sha.digest(ni.getHardwareAddress());
			secretKeySpec = new SecretKeySpec(key, "AES");
		    cipher = Cipher.getInstance("AES");		
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
		}
	 	
	}
	
	public synchronized String get(String key){
		synchronized(cipher){
			synchronized(prefs){
				 try {
					cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
					byte[] encKey = cipher.doFinal(key.getBytes());
					
					byte[] encVal = prefs.getByteArray(new String(encKey), null);
					
					if(encVal != null){
						cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
						byte[] decVal = cipher.doFinal(encVal);
						return new String(decVal);
					}
					
				} catch (Exception e){
					log.severe(e.getLocalizedMessage());
				}
			}
		}
		return null;
	}
	
	public synchronized void put(String key, String val){
		synchronized(cipher){
			synchronized(prefs){
				try {
					cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
					byte[] encKey = cipher.doFinal(key.getBytes());
					byte[] encVal = cipher.doFinal(key.getBytes());
					prefs.putByteArray(new String(encKey), encVal);
					prefs.flush();
				} catch (Exception e) {
					log.severe(e.getLocalizedMessage());
				}
			}
		}
	}
	
	public synchronized void wipe(){
		synchronized(prefs){
			try {
				prefs.removeNode();
				prefs.flush();
			} catch (BackingStoreException e) {
				log.severe(e.getLocalizedMessage());
			}
		}
	}

}
