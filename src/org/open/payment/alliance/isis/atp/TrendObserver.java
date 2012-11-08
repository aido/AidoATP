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
	private ATPTicker high;
	private ATPTicker low;
	private int bidArrow;
	private int askArrow;
	private int trendArrow;
	private Logger log;
	private ATPTicker lastTick;
	private long learnTime;
	private TickerManager tickerManager;
	private boolean quit;
	private CurrencyUnit localCurrency;
	
	public TrendObserver(TickerManager tickerManager) {
		this.tickerManager = tickerManager;
		quit = false;
		log = LoggerFactory.getLogger(TrendObserver.class);
		ArrayList<ATPTicker> ticker = tickerManager.getMarketData();
		localCurrency = tickerManager.getCurrency();
		if(ticker != null && !ticker.isEmpty()) {
			lastTick = ticker.get(ticker.size()-1);
			long now = System.currentTimeMillis();
			long oneMinsAgo = now - Constants.ONEMINUTE;
			if(lastTick.getTimestamp().isAfter(oneMinsAgo)) {
				learnTime = System.currentTimeMillis();
			}else {
				learnTime = System.currentTimeMillis() + Constants.ONEHOUR; //We don't want to jump the gun on trades.  Let it learn for an hour.
			}
		}else {
			learnTime = System.currentTimeMillis() + Constants.ONEHOUR; //We don't want to jump the gun on trades.  Let it learn for an hour.
		}
		
	}
	
	@Override
	public void run() {
		
		while(!quit) {
			//Each run, currently 1/Min
			
			//(Re)initialize variables
			trendArrow = 0;
			bidArrow = 0;
			askArrow = 0;
			
			//VWAP = Volume Weighted Average Price
			//Each (transaction price * transaction volume) / total volume
			//We are concerned not only with current vwap, but previous vwap.
			// This is because the differential between the two is an important market indicator
			
			vwap = BigMoney.zero(tickerManager.getCurrency());
			
			//We can't have multiple threads messing with the ticker object
			//Ideally this should have been a deep copy, but went this way for speed.
			ArrayList<ATPTicker> ticker = null;
			ATPTicker tick = null;
			while(ticker == null || ticker.isEmpty()) {
				ticker = tickerManager.getMarketData();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			
			synchronized(ticker) {
				
				//ticker - could be empty if there is no new data in over an hour, we've been disconnected, or the marketpolling thread has crashed.
				if(!ticker.isEmpty()) {
					low = ticker.get(0);
					high = ticker.get(0);
				}
				
				//Items in here are done once for every item in the ticker
				BigDecimal newVolume = null,absVolume = null,oldVolume = null,changedVolume = null;
				BigDecimal totalVolume = new BigDecimal("0");
				BigMoney oldPrice = null, newPrice = null;
				BigMoney newBid = null, oldBid = null;
				BigMoney newAsk = null, oldAsk = null;
				
				trendArrow = 0;
				bidArrow = 0;
				askArrow = 0;
				
				for(int idx =0; idx < ticker.size(); idx++){
					
					tick = ticker.get(idx);
					
					//The first thing we want to look at is the volume
					//We need a changed volume
					//Changed volume is new volume - old volume
					//We need 2 volumes, a total volume & an absolute volume
					
					
					
					if(idx == 0){
						
						oldVolume = BigDecimal.ZERO;
						oldPrice = BigMoney.zero(localCurrency);
						oldBid = BigMoney.zero(localCurrency);
						oldAsk = BigMoney.zero(localCurrency);
						
					}else{
						
						oldVolume = newVolume;
						oldPrice = newPrice;
						oldBid = newBid;
						oldAsk = newAsk;
						
					}
					
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
					
					if(newPrice.minus(oldPrice).isPositive()){
						trendArrow++;
					}else if(newPrice.minus(oldPrice).isNegative()){
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
				}
				
				vwap = vwap.dividedBy(totalVolume, RoundingMode.HALF_EVEN);
				lastTick = tick;
				
			}
			
			
			
			System.out.println("\nHigh: "+high.toString());
			System.out.println("Low: "+low.toString());
			System.out.println("Current: "+tick.toString());
			System.out.println("VWAP: "+vwap.getAmount().toPlainString());
			
			System.out.println("\n");
			if(System.currentTimeMillis() > learnTime) {
				evaluateMarketConditions();
			}else {
				log.info("Application has not run long enough to build a profile for "
				+localCurrency.getCurrencyCode()+" market.\n"
				+((learnTime - System.currentTimeMillis())/1000)/60+" minutes remaining.");
			}
			
			try {
				Thread.sleep(Constants.ONEMINUTE);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void evaluateMarketConditions() {
		new Thread(new TradingAgent(this)).start();
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
	public ATPTicker getLastTick() {
		return lastTick;
	}

	public void stop() {
		quit = true;
	}

	public TickerManager getTickerManager() {
		return tickerManager;
	}
}

