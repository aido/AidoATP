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
import com.xeiam.xchange.ExchangeFactory;
import com.xeiam.xchange.ExchangeSpecification;
import com.xeiam.xchange.mtgox.v1.MtGoxExchange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IsisMtGoxExchange extends MtGoxExchange {
	
	private static Exchange instance = null;
	private static Logger log = LoggerFactory.getLogger(IsisMtGoxExchange.class);
	private static ExchangeSpecification exchangeSpecification;
	
	public static Exchange getInstance() {
		if(instance == null) {
			instance = newInstance();
		}
		return instance;
	}
	
	public static Exchange newInstance() {	
			exchangeSpecification = new ExchangeSpecification("com.xeiam.xchange.mtgox.v1.MtGoxExchange");

			String apiKey = Application.getInstance().getConfig("ApiKey");
			String secretKey= Application.getInstance().getConfig("SecretKey");
			
			log.debug("MtGox API Key: "+apiKey);
			log.debug("MtGox Secret Key: "+secretKey);
			
			exchangeSpecification.setApiKey(apiKey);
			exchangeSpecification.setSecretKey(secretKey);
			exchangeSpecification.setUri("https://mtgox.com");
			exchangeSpecification.setHost("mtgox.com");
			exchangeSpecification.setVersion("1");
			instance = ExchangeFactory.INSTANCE.createExchange(exchangeSpecification);
		return instance;
	}
}