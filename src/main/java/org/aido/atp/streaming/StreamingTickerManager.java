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

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;

import org.joda.money.CurrencyUnit;

import com.xeiam.xchange.Currencies;
import com.xeiam.xchange.dto.marketdata.Ticker;
import com.xeiam.xchange.service.ExchangeEvent;
import com.xeiam.xchange.service.marketdata.streaming.StreamingMarketDataService;

/**
* Streaming Ticker Manager class.
*
* @author Aido
*/

public class StreamingTickerManager extends TickerManager {

	private StreamingMarketDataService marketData;
	private BlockingQueue<ExchangeEvent> eventQueue;
	private BlockingQueue<Ticker> tickerQueue;
	private String exchangeName;

	public StreamingTickerManager(CurrencyUnit currency, String exchangeName) {
		super(currency,exchangeName);
		this.exchangeName = exchangeName;
		try {
			marketData = ExchangeManager.getInstance(exchangeName).newExchange().getStreamingMarketDataService();
			// Get blocking queue that receives streaming ticker data
			tickerQueue = marketData.getTickerQueue(Currencies.BTC,currency.getCurrencyCode());
			// Get blocking queue that receives exchange event data
			eventQueue = marketData.getEventQueue();
		} catch (Exception e) {
			e.printStackTrace();
		} 	
	}
	
	public void getTick() {
		ExchangeEvent exchangeEvent;
		
		try {
			do {				
				// Exhaust exchange events first
				exchangeEvent = eventQueue.take();
//				log.debug(exchangeName + " Exchange event: {} {}", exchangeEvent.getEventType().name(), new String(exchangeEvent.getRawData()));
				log.debug("{} Exchange event: {}", exchangeName, exchangeEvent.getEventType().name());
			} while (!eventQueue.isEmpty());
			while (!tickerQueue.isEmpty()) {
				// Check for Tickers
				checkTick(tickerQueue.take());
			}
		} catch (Exception e) {
			log.error("ERROR: Caught unexpected {} exception, ticker manager shutting down now!. Details are listed below.", exchangeName);
			e.printStackTrace();
			stop();
		}
	}
}