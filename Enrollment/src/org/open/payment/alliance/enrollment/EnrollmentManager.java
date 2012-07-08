package org.open.payment.alliance.enrollment;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;

public class EnrollmentManager {

	private static EnrollmentManager instance = null;
	private static Logger log;
	private MessageDigest md;
	private EnrollmentManager(){
		log = Logger.getLogger(EnrollmentManager.class.getName());
	}
	
	public static EnrollmentManager getInstance() {
		if(instance == null){
			instance = new EnrollmentManager();
		}
		return instance;
	}
	
	/**
	 * This method computes the bitcoinAddress and enrollmentID hash
	 * @param enrollmentID
	 * @param userID
	 * @return bitcoinAddress or null if failed
	 */
	public synchronized EnrollmentModel enroll(byte[] enrollmentID, byte[] userID) {
		
		
		try {
			md = MessageDigest.getInstance("SHA-512");
			md.reset();
		} catch (NoSuchAlgorithmException e) {
			log.severe(e.getLocalizedMessage());
			return null;
		}
		
		
		//eID is used by the Messaging System as an AES key base for attempting to decrypt incoming messages
		byte[] eID = md.digest(md.digest(enrollmentID));
		md.reset();
		
		md.update(enrollmentID);
		md.update(userID);
		byte[] keyBytes = md.digest(md.digest());
		md.reset();
	
		//privKey is in fact the private key it is disposed of and calculated on the the fly
		BigInteger privKey = new BigInteger(1,keyBytes);
		
		ECKey key = new ECKey(privKey);
		
		Address addr = key.toAddress(NetworkParameters.prodNet());
	
		return new EnrollmentModel(eID,addr,privKey);
		
	}
}
