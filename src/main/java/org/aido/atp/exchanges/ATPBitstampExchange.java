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

import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.ExchangeFactory;
import com.xeiam.xchange.ExchangeSpecification;
import com.xeiam.xchange.bitstamp.BitstampExchange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* AidoATP Bitstamp class.
*
* @author Aido
*/

public class ATPBitstampExchange extends BitstampExchange {

	private static final String EXCHANGENAME = "Bitstamp";
	private static final String TICKERMANAGERCLASS = PollingTickerManager.class.getName();
	private static Exchange instance = null;
	private static Logger log = LoggerFactory.getLogger(ATPBitstampExchange.class);

	public static Exchange getInstance() {
		if(instance == null) {
			instance = newInstance();
		}
		return instance;
	}

	public static Exchange newInstance() {

		String userName = Application.getInstance().getConfig(EXCHANGENAME + "UserName");
		String passWord = Application.getInstance().getConfig(EXCHANGENAME + "Password");

		log.debug("{} UserName: {}",EXCHANGENAME,userName);
		log.debug("{} Password: {}",EXCHANGENAME,passWord);
		
		Exchange exchange = ExchangeFactory.INSTANCE.createExchange(BitstampExchange.class.getName());
		
		ExchangeSpecification exchangeSpecification = exchange.getDefaultExchangeSpecification();
		exchangeSpecification.setUserName(userName);
		exchangeSpecification.setPassword(passWord);
		exchange.applySpecification(exchangeSpecification);

		ExchangeManager.getInstance(EXCHANGENAME).setExchangeSpecification(exchangeSpecification);
		ExchangeManager.getInstance(EXCHANGENAME).setTickerManagerClass(TICKERMANAGERCLASS);
		
		log.info("Connecting to {} Exchange",EXCHANGENAME);
			
		return exchange;
	}

	public static String getExchangeName() {
		return EXCHANGENAME;
	}
}