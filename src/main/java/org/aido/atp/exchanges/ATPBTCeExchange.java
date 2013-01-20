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
import com.xeiam.xchange.btce.BTCEExchange;

import org.joda.money.CurrencyUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* AidoATP BTCeExchange class.
*
* @author Aido
*/

public class ATPBTCeExchange extends BTCEExchange {
	
	private static Exchange instance = null;
	private static Logger log = LoggerFactory.getLogger(ATPBTCeExchange.class);
	private static ExchangeSpecification exchangeSpecification;
	
	public static Exchange getInstance() {
		if(instance == null) {
			instance = newInstance();
		}
		return instance;
	}
	
	public static Exchange newInstance() {	
			exchangeSpecification = new ExchangeSpecification("com.xeiam.xchange.btce.BTCEExchange");

			String apiKey = Application.getInstance().getConfig("BTC-eApiKey");
			String secretKey= Application.getInstance().getConfig("BTC-eSecretKey");
			
			log.debug("BTC-e API Key: "+apiKey);
			log.debug("BTC-e Secret Key: "+secretKey);
			
			exchangeSpecification.setApiKey(apiKey);
			exchangeSpecification.setSecretKey(secretKey);			
			exchangeSpecification.setUri("https://btc-e.com");
			exchangeSpecification.setHost("btc-e.com");
			exchangeSpecification.setPort(80);
			instance = ExchangeFactory.INSTANCE.createExchange(exchangeSpecification);
			ExchangeManager.getInstance("BTC-e").setExchangeSpecification(exchangeSpecification);
			log.info("Connecting to BTC-e Exchange");
		return instance;
	}
}