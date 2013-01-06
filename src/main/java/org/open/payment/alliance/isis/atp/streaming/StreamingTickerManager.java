/**
 * Copyright (c) 2013 Aido
 * 
 * This file is part of Isis ATP.
 * 
 * Isis ATP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Isis ATP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Isis ATP.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.open.payment.alliance.isis.atp;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;

import org.joda.money.CurrencyUnit;

import com.xeiam.xchange.Currencies;
import com.xeiam.xchange.dto.marketdata.Ticker;
import com.xeiam.xchange.service.marketdata.streaming.StreamingMarketDataService;

/**
* @author Auberon
*
*/
public class StreamingTickerManager extends TickerManager {

	private StreamingMarketDataService marketData;
	private BlockingQueue<Ticker> tickerQueue;

	public StreamingTickerManager(CurrencyUnit currency) {
		super(currency);
		try {
			marketData = ExchangeManager.getInstance().newExchange().getStreamingMarketDataService();
			tickerQueue = marketData.getTickerQueue(Currencies.BTC,currency.getCurrencyCode());
		}
		catch (Exception e) {
			e.printStackTrace();
		} 	
	}
	
	public void getTick() {
			try {
				checkTick(tickerQueue.take());
			} catch (Exception e) {
				log.error("ERROR: Caught unexpected exception, ticker manager shutting down now!. Details are listed below.");
				e.printStackTrace();
				stop();
			}
	}
}