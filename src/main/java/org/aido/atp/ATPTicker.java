/**
 * Copyright (c) 2013 Aido
 * 
 * This file is part of Aido ATP.
 * 
 * Aido ATP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Aido ATP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Aido ATP.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.aido.atp;

import java.io.Serializable;
import java.util.Date;

import org.joda.money.BigMoney;

import com.xeiam.xchange.dto.marketdata.Ticker;

/**
* ATP ticker class.
*
* @author Auberon
*/

public class ATPTicker implements Serializable{
	
	private static final long serialVersionUID = 3857496442807778974L;
	private BigMoney   last;
	private BigMoney   ask;
	private BigMoney   bid;
	private long       volume;
	private Date	   timestamp;
	private String     tradeableIdentifier;
	
	public ATPTicker(Ticker tick) {
		
		this.setLast(tick.getLast());
		this.setAsk(tick.getAsk());
		this.setBid(tick.getBid());
		this.setVolume(tick.getVolume().longValue());
		this.setTimestamp(tick.getTimestamp());
		this.setTradeableIdentifier(tick.getTradableIdentifier());
	}

	public BigMoney getLast() {
		return last;
	}

	public void setLast(BigMoney last) {
		this.last = last;
	}

	public BigMoney getAsk() {
		return ask;
	}

	public void setAsk(BigMoney ask) {
		this.ask = ask;
	}

	public BigMoney getBid() {
		return bid;
	}

	public void setBid(BigMoney bid) {
		this.bid = bid;
	}

	public long getVolume() {
		return volume;
	}

	public void setVolume(long volume) {
		this.volume = volume;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public String getTradeableIdentifier() {
		return tradeableIdentifier;
	}

	public void setTradeableIdentifier(String tradeableIdentifier) {
		this.tradeableIdentifier = tradeableIdentifier;
	}
	
	@Override
	public String toString() {
		
		StringBuilder str = new StringBuilder();
		
		str.append(" Last: ");
		str.append(last.toString());
		str.append(" | ");
		
		str.append("Bid: ");
		str.append(bid.toString());
		str.append(" | ");
		
		str.append("Ask: ");
		str.append(ask.toString());
		str.append(" | ");
		
		str.append("Volume: ");
		str.append(volume);
		str.append(" | ");
		
		str.append("Currency: ");
		str.append(tradeableIdentifier);
		str.append(" | ");

		str.append("TimeStamp: ");
		str.append(timestamp.toString());
		
		return str.toString();
	}
}
