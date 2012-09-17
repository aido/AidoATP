package org.open.payment.alliance.isis.atp;

import java.io.Serializable;
import org.joda.money.BigMoney;
import org.joda.time.DateTime;

import com.xeiam.xchange.dto.marketdata.Ticker;

/**
 * This class is only needed because the author of xchange API decided to make his Ticker class final and not serializable! grrrr
 * @author Auberon
 *
 */
public class ATPTicker implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3857496442807778974L;
	private BigMoney   last;
	private BigMoney   ask;
	private BigMoney   bid;
	private long       volume;
	private DateTime   timestamp;
	private String     tradeableIdentifier;
	
	public ATPTicker(
			BigMoney   last,
			BigMoney   ask,
			BigMoney   bid,
			long       volume,
			DateTime   timestamp,
			String     tradeableIdentifier) {
		
			this.setLast(last);
			this.setAsk(ask);
			this.setBid(bid);
			this.setVolume(volume);
			this.setTimestamp(timestamp);
			this.setTradeableIdentifier(tradeableIdentifier);
		
	}

	public ATPTicker(Ticker tick) {
		this.setLast(tick.getLast());
		this.setAsk(tick.getAsk());
		this.setBid(tick.getBid());
		this.setVolume(tick.getVolume());
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

	public DateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(DateTime timestamp) {
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
