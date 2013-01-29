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

import org.joda.money.CurrencyUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* AidoATP Bitstamp class.
*
* @author Aido
*/

public class ATPBitstampExchange extends BitstampExchange {
	
	private static Exchange instance = null;
	private static Logger log = LoggerFactory.getLogger(ATPBitstampExchange.class);
	private static ExchangeSpecification exchangeSpecification;
	
	public static Exchange getInstance() {
		if(instance == null) {
			instance = newInstance();
		}
		return instance;
	}
	
	public static Exchange newInstance() {	
			exchangeSpecification = new ExchangeSpecification(BitstampExchange.class.getName());

			String userName = Application.getInstance().getConfig("BitstampUserName");
			String passWord = Application.getInstance().getConfig("BitstampPassword");
			
			log.debug("Bitstamp UserName: {}",userName);
			log.debug("Bitstamp Password: {}",passWord);
			
			exchangeSpecification.setUserName(userName);
			exchangeSpecification.setPassword(passWord);

			exchangeSpecification.setUri("https://www.bitstamp.net");
			exchangeSpecification.setHost("www.bitstamp.net");
			instance = ExchangeFactory.INSTANCE.createExchange(exchangeSpecification);
			ExchangeManager.getInstance("Bitstamp").setExchangeSpecification(exchangeSpecification);
			log.info("Connecting to Bitstamp Exchange");
		return instance;
	}
}