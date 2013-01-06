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

import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.ExchangeSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExchangeManager implements Runnable {

	private static ExchangeManager instance = null;
	private static Logger log;
	private Exchange exchange;
	private ExchangeSpecification exchangeSpecification;
	
	public static ExchangeManager getInstance() {
		if(instance == null) {
			instance = new ExchangeManager();
		}
		return instance;
	}
	
	private ExchangeManager(){
		log = LoggerFactory.getLogger(ExchangeManager.class);
	}
	
	@Override
	public synchronized void run() {
		exchange = IsisMtGoxExchange.getInstance();
	}
	
	public Exchange getExchange() {
		return exchange;
	}

	public Exchange newExchange() {
		return IsisMtGoxExchange.newInstance();
	}

	public void setExchangeSpecification(ExchangeSpecification exchangeSpecification) {
		this.exchangeSpecification = exchangeSpecification;
	}

	public String getHost() {
		return exchangeSpecification.getHost();
	}
	
	public int getPort() {
		return exchangeSpecification.getPort();
	}
}