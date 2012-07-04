package org.open.payment.alliance.enrollment;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
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
	public synchronized byte[] enroll(byte[] enrollmentID, byte[] userID,MessageQ enrollmentQ) {
		
		
		try {
			md = MessageDigest.getInstance("SHA-512");
			md.reset();
		} catch (NoSuchAlgorithmException e) {
			log.log(Level.SEVERE, Application.getError("NO_SUCH_ALGORITHIM"));
			return null;
		}
		
		
		//eIDHash is used by the Messaging System as an AES key base for attempting to decrypt incoming messages
		byte[] eIDHash = md.digest(md.digest(enrollmentID));
		md.reset();
		
		md.update(enrollmentID);
		md.update(userID);
		byte[] keyBase = md.digest(md.digest());
		md.reset();
	
		//privKey is in fact the private key it is disposed of and calculated on the the fly
		BigInteger privKey = new BigInteger(1,keyBase);
		
		ECKey key = new ECKey(privKey);
		
		Address addr = key.toAddress(NetworkParameters.prodNet());
		
		//
		enrollmentQ.send(eIDHash,userID,addr);
		
		return addr.getHash160();
		
	}

	private void sendToQ(byte[] eIDHash, byte[] userID, Address addr) {
		// TODO Auto-generated method stub
		
	}
	

}
