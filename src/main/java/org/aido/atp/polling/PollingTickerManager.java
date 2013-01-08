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
import java.util.concurrent.TimeUnit;
import java.net.Socket;

import org.joda.money.CurrencyUnit;

import com.xeiam.xchange.Currencies;
import com.xeiam.xchange.service.marketdata.polling.PollingMarketDataService;

/**
* Polling Ticker Manager class.
*
* @author Aido
*/

public class PollingTickerManager extends TickerManager{
	
	private PollingMarketDataService marketData;
	private CurrencyUnit currency;

	public PollingTickerManager(CurrencyUnit currency) {
		super(currency);
		this.currency = currency;
		try {
			marketData = ExchangeManager.getInstance().getExchange().getPollingMarketDataService();			
		} catch (Exception e) {
			e.printStackTrace();
		} 	
	}

	public void getTick() {
		try {
			checkTick(marketData.getTicker(Currencies.BTC, currency.getCurrencyCode()));
			TimeUnit.SECONDS.sleep(10);
		} catch (com.xeiam.xchange.PacingViolationException | com.xeiam.xchange.HttpException e) {
			Socket testSock = null;
			while (true) {
				try {
					log.warn("WARNING: Testing connection to exchange");
					testSock = new Socket(ExchangeManager.getInstance().getHost(),ExchangeManager.getInstance().getPort());
					if (testSock != null) { break; }
				}
				catch (java.io.IOException e1) {
					try {
						log.error("ERROR: Cannot connect to exchange. Sleeping for one minute");
						TimeUnit.MINUTES.sleep(1);
					} catch (InterruptedException e2) {
						e2.printStackTrace();
					}
				}
			}
		} catch (Exception e) {
			log.error("ERROR: Caught unexpected exception, ticker manager shutting down now!. Details are listed below.");
			e.printStackTrace();
			stop();
		}
	}
	
}