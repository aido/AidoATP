/**
 * 
 */
package org.open.payment.alliance.enrollment;

import com.google.bitcoin.core.Address;

/**
 * @author Auberon
 * This interface defines a MessageQ interface for enrollment, which can be used to generate and send the enrollment message to the datastore(s)
 * 
 */
public interface MessageQ {

	void send(byte[] privateKey, byte[] userID, Address addr);
	
}
