/**
 * 
 */
package org.open.payment.alliance.isis.atp;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;

import org.joda.time.DateTime;

import com.xeiam.xchange.Currencies;
import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.dto.marketdata.Ticker;
import com.xeiam.xchange.service.marketdata.polling.PollingMarketDataService;
import com.xeiam.xchange.service.trade.polling.PollingTradeService;

/**
 * @author steve
 *
 */
public class TickerManager implements Runnable{
	

	private PollingMarketDataService marketData;
	BlockingQueue<Ticker> tickerQ;
	private long currentVolume;
	private long lastVolume;
	private static  ArrayList<Ticker> tickerCache;
	

	 TickerManager(Exchange mtgox, PollingTradeService tradeService) {
		tickerCache = new ArrayList<Ticker>();
		marketData = mtgox.getPollingMarketDataService();
	}

	@Override
	public void run() {
		
		while(true){
			try {
				Ticker tick = marketData.getTicker(Currencies.BTC, Currencies.USD);
				lastVolume = currentVolume;
				currentVolume = tick.getVolume();
				if(currentVolume != lastVolume) {
					synchronized(tickerCache) {
						tickerCache.add(tick);
					}
					//System.out.println(tick.toString());
				}
				Thread.sleep(Constants.TENSECONDS);
			} catch (Exception e) {
				System.err.println("Caught unexpected exception, shutting down now!.\nDetails are listed below.");
				e.printStackTrace();
				System.exit(1);
			}
		}
		
	}

	public static ArrayList<Ticker> getMarketData(){
		
		synchronized(tickerCache) {
			ArrayList<Ticker> removeList = new ArrayList<Ticker>();
			DateTime now = new DateTime();
			for(Ticker tick : tickerCache){
				DateTime time = tick.getTimestamp();
				if(now.getMillis() - time.getMillis() > Constants.ONEHOUR) {
					removeList.add(tick);
				}
			}
			tickerCache.removeAll(removeList);
		}
		return tickerCache;
	}
	
	
}
