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
import com.xeiam.xchange.service.ExchangeEvent;
import com.xeiam.xchange.service.ExchangeEventType;
import com.xeiam.xchange.service.marketdata.streaming.StreamingMarketDataService;
import com.xeiam.xchange.dto.marketdata.Ticker;

/**
* Streaming Ticker Manager class.
*
* @author Aido
*/

public class StreamingTickerManager extends TickerManager {

	private BlockingQueue<ExchangeEvent> eventQueue;
	private String exchangeName;

	public StreamingTickerManager(CurrencyUnit currency, String exchangeName) {
		super(currency,exchangeName);
		this.exchangeName = exchangeName;
		
		try {
			StreamingMarketDataService marketData = ExchangeManager.getInstance(exchangeName).newExchange().getStreamingMarketDataService();
			// Get blocking queue that receives exchange event data
			eventQueue = marketData.getEventQueue(Currencies.BTC,currency.getCurrencyCode());
		} catch (Exception e) {
			e.printStackTrace();
		} 	
	}
	
	public void getTick() {
		ExchangeEvent exchangeEvent;
		ExchangeEventType exchangeEventType;
		
		try {
			// Monitor the exchange events
			exchangeEvent = eventQueue.take();
			exchangeEventType = exchangeEvent.getEventType();

			if (exchangeEventType == ExchangeEventType.TICKER) {
//				Ticker ticker = (Ticker) exchangeEvent.getPayload();
				checkTick((Ticker) exchangeEvent.getPayload());
//			} else if (exchangeEventType == ExchangeEventType.DISCONNECT)) {
//				log.error("{} has disconnected", exchangeName);
			} else {
				log.debug(exchangeName + " Exchange event: {} {}",exchangeEventType.name(),exchangeEvent.getData());
			}
		} catch (Exception e) {
			log.error("ERROR: Caught unexpected {} exception, ticker manager shutting down now!. Details are listed below.",exchangeName);
			e.printStackTrace();
			stop();
		}
	}
}