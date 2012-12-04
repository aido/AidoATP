package org.open.payment.alliance.isis.atp;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;

import org.joda.money.BigMoney;
import org.joda.money.CurrencyUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrendObserver implements Runnable {

	private BigMoney vwap;
	private BigMoney shortSMA;
	private BigMoney longSMA;
	private BigMoney shortEMA;
	private BigMoney longEMA;
	private ATPTicker high;
	private ATPTicker low;
	private ArrayList<ATPTicker> ticker;
	private Integer bidArrow;
	private Integer askArrow;
	private Integer trendArrow;
	private Logger log;
	private boolean learningComplete;
	private StreamingTickerManager tickerManager;
	private CurrencyUnit localCurrency;
	
	public TrendObserver(StreamingTickerManager tickerManager) {
		this.tickerManager = tickerManager;
		log = LoggerFactory.getLogger(TrendObserver.class);
		ticker = tickerManager.getMarketData();
		localCurrency = tickerManager.getCurrency();
		learningComplete = false;
		if(ticker != null && !ticker.isEmpty()) {
			if (ticker.size() < Integer.valueOf(Application.getInstance().getConfig("minTickSize"))){
				log.info("Trend observer does not currently have enough "+localCurrency.getCurrencyCode()+" data to determine trend. "+localCurrency.getCurrencyCode()+" Ticker size: "+ticker.size());
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
		
		Integer shortMASize = Integer.valueOf(Application.getInstance().getConfig("shortMATickSize"));
		Integer idx = 0;
		double expShortEMA = 0;
		double expLongEMA = 0;
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
		
		vwap = BigMoney.zero(tickerManager.getCurrency());
		
		synchronized(ticker) {
			
			//ticker - could be empty if there is no new data in over an hour, we've been disconnected, or the TickerManager thread has crashed.
			if(!ticker.isEmpty()) {
				low = ticker.get(0);
				high = ticker.get(0);
				if (shortMASize > ticker.size()) {
					shortMASize = ticker.size();
				}
				shortEMA = ticker.get(ticker.size() - shortMASize).getLast();
				longEMA = ticker.get(0).getLast();
				expShortEMA = (double) 2 / (shortMASize + 1);
				expLongEMA = (double) 2 / (ticker.size() + 1);
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
				
				if ( idx >= ticker.size() - shortMASize ) {
					sumShortSMA = sumShortSMA.plus(newPrice);
					shortEMA = newPrice.multipliedBy(expShortEMA).plus(shortEMA.multipliedBy(1 - expShortEMA));
				}
				
				sumLongSMA = sumLongSMA.plus(newPrice);
				longEMA = newPrice.multipliedBy(expLongEMA).plus(longEMA.multipliedBy(1 - expLongEMA));
				
				idx++;
			}
			vwap = vwap.dividedBy(totalVolume, RoundingMode.HALF_EVEN);
			shortSMA = sumShortSMA.dividedBy(Long.valueOf(shortMASize),RoundingMode.HALF_UP);
			longSMA = sumLongSMA.dividedBy(Long.valueOf(ticker.size()),RoundingMode.HALF_UP);
		}
		
		log.info("High "+localCurrency.getCurrencyCode()+" :- "+high.toString());
		log.info("Low "+localCurrency.getCurrencyCode()+" :- "+low.toString());			
		log.info("Current "+localCurrency.getCurrencyCode()+" :- "+ticker.get(ticker.size() - 1).toString());
		log.info("VWAP "+localCurrency.getCurrencyCode()+" : "+vwap.getAmount().toPlainString());
		
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
	
	public ATPTicker getLastTick() {
		return ticker.get(ticker.size() - 1);
	}
	
	public Integer getTickerSize() {
		return ticker.size();
	}

	public StreamingTickerManager getTickerManager() {
		return tickerManager;
	}
}