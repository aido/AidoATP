/**
 * 
 */
package org.open.payment.alliance.enrollment;

import java.math.BigInteger;

import com.google.bitcoin.core.Address;

/**
 * @author Auberon
 *
 */
public class EnrollmentModel {

	private byte[] eID;
	private Address addr;
	private BigInteger privKey;
	
	public EnrollmentModel(byte[] eID, Address addr, BigInteger privKey) {
		this.eID = eID;
		this.addr = addr;
		this.privKey = privKey;
	}

	public byte[] getEiD(){
		return eID;
	}
	
	public Address getAddr(){
		return addr;
	}
	
	public BigInteger getKey(){
		return privKey;
	}
}
