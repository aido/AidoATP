/**
*
*/
package org.open.payment.alliance.isis.atp;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;

import org.joda.money.CurrencyUnit;

import com.xeiam.xchange.Currencies;
import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.dto.marketdata.Ticker;
import com.xeiam.xchange.service.marketdata.streaming.StreamingMarketDataService;

/**
* @author Auberon
*
*/
public class StreamingTickerManager extends TickerManager{
	
	private StreamingMarketDataService marketData;
	private BlockingQueue<Ticker> tickerQueue;
					
	public StreamingTickerManager(CurrencyUnit currency) {
		super(currency);
		try {
			Exchange exchange = com.xeiam.xchange.mtgox.v1.MtGoxExchange.newInstance();
			marketData = exchange.getStreamingMarketDataService();
			tickerQueue = marketData.requestTicker(Currencies.BTC, currency.getCurrencyCode());
		}
		catch (Exception e) {
			e.printStackTrace();
		} 	
	}

	@Override
	public synchronized void run() {
		while(!getQuit()){
			try {
				checkTick(tickerQueue.take());
			} catch (Exception e) {
				getLog().error("ERROR: Caught unexpected exception, ticker manager shutting down now!. Details are listed below.");
				e.printStackTrace();
				stop();
			}
		}
	}
}