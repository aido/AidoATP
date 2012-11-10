/**
* 
*/
package org.open.payment.alliance.isis.atp;

import java.math.BigDecimal;

import org.joda.money.BigMoney;

/**
* @author Auberon
* A simple model to represent profit/loss for a given currency
* 
*/
public class PLModel {
	
	private BigMoney amount;
	private BigDecimal percent;

	public PLModel(BigMoney amount, BigDecimal percent) {
		this.amount = amount;
		this.percent = percent;
	}
	
	public BigMoney getAmount() {
		return amount;
	}
	
	public BigDecimal getPercent() {
		return percent;
	}
	
}
