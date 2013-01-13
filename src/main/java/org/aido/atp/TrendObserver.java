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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;

import org.joda.money.BigMoney;
import org.joda.money.CurrencyUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* Trend Observer class.
*
* @author Aido
*/

public class TrendObserver implements Runnable {

	private BigMoney vwap;
	private BigMoney shortSMA;
	private BigMoney longSMA;
	private BigMoney shortEMA;
	private BigMoney longEMA;
	private BigMoney shortMACD;
	private BigMoney longMACD;
	private BigMoney sigLineMACD;
	private ATPTicker high;
	private ATPTicker low;
	private ATPTicker last;
	private ArrayList<ATPTicker> ticker;
	private int bidArrow;
	private int askArrow;
	private int trendArrow;
	private int tickerSize;
	private Logger log;
	private boolean learningComplete;
	private CurrencyUnit localCurrency;
	
	public TrendObserver(ArrayList<ATPTicker> marketData) {
		log = LoggerFactory.getLogger(TrendObserver.class);
		this.ticker = marketData;
		localCurrency = ticker.get(0).getLast().getCurrencyUnit();
		learningComplete = false;
		if(ticker != null && !ticker.isEmpty()) {
			if (ticker.size() < Integer.parseInt(Application.getInstance().getConfig("MinTickSize"))){
				log.info(localCurrency.getCurrencyCode()+" Ticker size: "+ticker.size()+". Trend observer does not currently have enough "+localCurrency.getCurrencyCode()+" data to determine trend.");
				learningComplete = false;
			} else {
				learningComplete = true;
			}
		} else {
			log.info("Trend observer currently has no "+localCurrency.getCurrencyCode()+" ticker data");
			learningComplete = false;
		}	
	}
	
	@Override
	public void run() {
		//(Re)initialize variables
		trendArrow = 0;
		bidArrow = 0;
		askArrow = 0;
		shortSMA = BigMoney.zero(localCurrency);
		longSMA = BigMoney.zero(localCurrency);
		shortEMA = BigMoney.zero(localCurrency);
		longEMA = BigMoney.zero(localCurrency);
		shortMACD = BigMoney.zero(localCurrency);
		longMACD = BigMoney.zero(localCurrency);
		sigLineMACD = BigMoney.zero(localCurrency);
		
		int idx = 0;
		int shortMASize = Integer.parseInt(Application.getInstance().getConfig("ShortMATickSize"));
		int shortMACDSize = Integer.parseInt(Application.getInstance().getConfig("ShortMACDTickSize"));
		int longMACDSize = Integer.parseInt(Application.getInstance().getConfig("LongMACDTickSize"));
		int sigLineMACDSize = Integer.parseInt(Application.getInstance().getConfig("SigLineMACDSize"));
		double expShortEMA = 0;
		double expLongEMA = 0;
		double expShortMACD = 0;
		double expLongMACD = 0;
		double expSigLineMACD = 0;
		BigMoney sumShortSMA = BigMoney.zero(localCurrency);
		BigMoney sumLongSMA = BigMoney.zero(localCurrency);	

		//Items in here are done once for every item in the ticker
		BigMoney newBid = null, oldBid = BigMoney.zero(localCurrency);
		BigMoney newAsk = null, oldAsk = BigMoney.zero(localCurrency);
		BigMoney newPrice = null, oldPrice = BigMoney.zero(localCurrency);
		BigDecimal newVolume = null, oldVolume = BigDecimal.ZERO;
		BigDecimal totalVolume = BigDecimal.ZERO, absVolume = null, changedVolume = null;
		
		//VWAP = Volume Weighted Average Price
		//Each (transaction price * transaction volume) / total volume
		//We are concerned not only with current vwap, but previous vwap.
		// This is because the differential between the two is an important market indicator
		
		vwap = BigMoney.zero(localCurrency);
		
		synchronized(ticker) {
			
			//ticker - could be empty if there is no new data in over an hour, we've been disconnected, or the TickerManager thread has crashed.
			if(!ticker.isEmpty()) {
				tickerSize = ticker.size();
				low = ticker.get(0);
				high = ticker.get(0);
				last = ticker.get(tickerSize - 1);
				
				if (shortMASize > tickerSize) {
					shortMASize = tickerSize;
				}
				shortEMA = ticker.get(tickerSize - shortMASize).getLast();

				if (shortMACDSize > tickerSize) {
					shortMACDSize = tickerSize;
				}
				shortMACD = ticker.get(tickerSize - shortMACDSize).getLast();

				if (longMACDSize > tickerSize) {
					longMACDSize = tickerSize;
				}
				longMACD = ticker.get(tickerSize - longMACDSize).getLast();
				sigLineMACD = shortMACD.minus(longMACD);

				longEMA = ticker.get(0).getLast();
				expShortEMA = (double) 2 / (shortMASize + 1);
				expLongEMA = (double) 2 / (tickerSize + 1);
				expShortMACD = (double) 2 / (shortMACDSize + 1);
				expLongMACD = (double) 2 / (longMACDSize + 1);
				expSigLineMACD = (double) 2 / (sigLineMACDSize + 1);
			}

			for(ATPTicker tick : ticker){		
				
				//The first thing we want to look at is the volume
				//We need a changed volume
				//Changed volume is new volume - old volume
				//We need 2 volumes, a total volume & an absolute volume
				
				//The volume of this tick, by itself
				newVolume = new BigDecimal(new BigInteger(""+tick.getVolume()));
				changedVolume = newVolume.subtract(oldVolume);
				absVolume = changedVolume.abs();
				
				newPrice = tick.getLast();
				newBid = tick.getBid();
				newAsk = tick.getAsk();
		
				if(newPrice.isGreaterThan(high.getLast())){					
					high = tick;
				}else if(newPrice.isLessThan(low.getLast())){
					low = tick;
				}
				
				if(newPrice.isGreaterThan(oldPrice)){
					trendArrow++;
				}else if(newPrice.isLessThan(oldPrice)){
					trendArrow--;
				}
				
				if(newBid.isGreaterThan(oldBid)){
					bidArrow++;
				}else if(newBid.isLessThan(oldBid)){
					bidArrow--;
				}
				
				if(newAsk.isGreaterThan(oldAsk)){
					askArrow++;
				}else if(newAsk.isLessThan(oldAsk)){
					askArrow--;
				}
				
				vwap = vwap.plus(newPrice.multipliedBy(absVolume));
				totalVolume = totalVolume.add(absVolume);
				
				oldVolume = newVolume;
				oldPrice = newPrice;
				oldBid = newBid;
				oldAsk = newAsk;

				if ( idx >= tickerSize - shortMASize ) {
					sumShortSMA = sumShortSMA.plus(newPrice);
					shortEMA = newPrice.multipliedBy(expShortEMA).plus(shortEMA.multipliedBy(1 - expShortEMA));
				}
				
				if ( idx >= tickerSize - shortMACDSize ) {
					shortMACD = newPrice.multipliedBy(expShortMACD).plus(shortMACD.multipliedBy(1 - expShortMACD));
				}

				if ( idx >= tickerSize - longMACDSize ) {
					longMACD = newPrice.multipliedBy(expLongMACD).plus(longMACD.multipliedBy(1 - expLongMACD));
				}
				
				sigLineMACD = shortMACD.minus(longMACD).multipliedBy(expSigLineMACD).plus(sigLineMACD.multipliedBy(1 - expSigLineMACD));

				sumLongSMA = sumLongSMA.plus(newPrice);
				longEMA = newPrice.multipliedBy(expLongEMA).plus(longEMA.multipliedBy(1 - expLongEMA));

				idx++;
			}
			vwap = vwap.dividedBy(totalVolume, RoundingMode.HALF_EVEN);
			shortSMA = sumShortSMA.dividedBy(Long.valueOf(shortMASize),RoundingMode.HALF_EVEN);
			longSMA = sumLongSMA.dividedBy(Long.valueOf(tickerSize),RoundingMode.HALF_EVEN);
		}
		
		log.info("High "+localCurrency.getCurrencyCode()+" :- "+high.toString());
		log.info("Low "+localCurrency.getCurrencyCode()+" :- "+low.toString());			
		log.info("Current "+localCurrency.getCurrencyCode()+" :- "+ticker.get(tickerSize - 1).toString());
		
		if(learningComplete) {
			log.debug("Starting "+localCurrency.getCurrencyCode()+" trend trading agent.");
			new Thread(new TrendTradingAgent(this)).start();
		}
	}

	public int getTrendArrow() {
		return trendArrow;
	}
	
	public int getBidArrow() {
		return bidArrow;
	}
	
	public int getAskArrow() {
		return askArrow;
	}
	
	public BigMoney getVwap() {
		return vwap;
	}
	
	public BigMoney getShortSMA() {
		return shortSMA;
	}
	
	public BigMoney getLongSMA() {
		return longSMA;
	}
	
	public BigMoney getShortEMA() {
		return shortEMA;
	}
	
	public BigMoney getLongEMA() {
		return longEMA;
	}

	public BigMoney getShortMACD() {
		return shortMACD;
	}
	
	public BigMoney getLongMACD() {
		return longMACD;
	}

	public BigMoney getSigLineMACD() {
		return sigLineMACD;
	}

	public ATPTicker getLastTick() {
		return last;
	}
	
	public int getTickerSize() {
		return tickerSize;
	}
	
	public CurrencyUnit getCurrency() {
		return localCurrency;
	}
}