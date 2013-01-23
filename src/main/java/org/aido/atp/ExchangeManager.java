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

import java.util.HashMap;

import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.ExchangeSpecification;

import org.joda.money.CurrencyUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* Exchange manager class.
*
* @author Aido
*/

public class ExchangeManager implements Runnable {

	private static Logger log;
	private Exchange exchange;
	private ExchangeSpecification exchangeSpecification;
	private HashMap<CurrencyUnit, Double> asksInARow;
	private HashMap<CurrencyUnit, Double> bidsInARow;
	private static HashMap<String, ExchangeManager> instances = new HashMap<String, ExchangeManager>();
	private String exchangeName;
	
	public static ExchangeManager getInstance(String exchangeName) {
		if(instances.get(exchangeName) == null)
			instances.put(exchangeName, new ExchangeManager(exchangeName));
		return instances.get(exchangeName);
	}
	
	private ExchangeManager(String exchangeName){
		this.exchangeName = exchangeName;
		log = LoggerFactory.getLogger(ExchangeManager.class);
		asksInARow = new HashMap<CurrencyUnit, Double>();
		bidsInARow = new HashMap<CurrencyUnit, Double>();
	}
	
	@Override
	public synchronized void run() {
		if (Application.getInstance().getConfig("Use" + exchangeName).equals("1")) {
			if (exchangeName.equals("MtGox")) {
				exchange = ATPMtGoxExchange.getInstance();
			} else if (exchangeName.equals("BTC-e")) {
				exchange = ATPBTCeExchange.getInstance();
			} else if (exchangeName.equals("Bitstamp")) {
				exchange = ATPBitstampExchange.getInstance();
			}
			getAccount();
		}
	}
	
	public Exchange getExchange() {
		return exchange;
	}

	public Exchange newExchange() {
		if (exchangeName.equals("MtGox")) {
			exchange = ATPMtGoxExchange.newInstance();
		} else if (exchangeName.equals("BTC-e")) {
			exchange = ATPBTCeExchange.newInstance();
		} else if (exchangeName.equals("Bitstamp")) {
				exchange = ATPBitstampExchange.newInstance();
		}
		return exchange;
	}
	public void setExchangeSpecification(ExchangeSpecification exchangeSpecification) {
		this.exchangeSpecification = exchangeSpecification;
	}

	public void getAccount() {
		Thread accountManagerThread = new Thread(AccountManager.getInstance(exchangeName));
		accountManagerThread.start();
	}

	public String getHost() {
		return exchangeSpecification.getHost();
	}
	
	public int getPort() {
		return exchangeSpecification.getPort();
	}
	
	public HashMap<CurrencyUnit, Double> getAsksInARow() {
		return asksInARow;
	}
	
	public void setAsksInARow(HashMap<CurrencyUnit, Double> asksInARow) {
		this.asksInARow = asksInARow;
	}
	
	public HashMap<CurrencyUnit, Double> getBidsInARow() {
		return bidsInARow;
	}

	public void setBidsInARow(HashMap<CurrencyUnit, Double> bidsInARow) {
		this.bidsInARow = bidsInARow;
	}
}