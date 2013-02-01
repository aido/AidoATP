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
import com.xeiam.xchange.bitcoincentral.BitcoinCentralExchange;

import org.joda.money.CurrencyUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* AidoATP BitcoinCentralExchange class.
*
* @author Aido
*/

public class ATPBitcoinCentralExchange extends BitcoinCentralExchange {

	private static Exchange instance = null;
	private static Logger log = LoggerFactory.getLogger(ATPBitcoinCentralExchange.class);
	private static ExchangeSpecification exchangeSpecification;

	public static Exchange getInstance() {
		if(instance == null) {
			instance = newInstance();
		}
		return instance;
	}

	public static Exchange newInstance() {
			exchangeSpecification = new ExchangeSpecification(BitcoinCentralExchange.class.getName());

			String userName = Application.getInstance().getConfig("BitcoinCentralUserName");
			String passWord = Application.getInstance().getConfig("BitcoinCentralPassword");

			log.debug("Bitcoin Central UserName: {}",userName);
			log.debug("Bitcoin Central Password: {}",passWord);

			exchangeSpecification.setUserName(userName);
			exchangeSpecification.setPassword(passWord);
			exchangeSpecification.setUri("https://bitcoin-central.net");
			exchangeSpecification.setHost("bitcoin-central.net");
			instance = ExchangeFactory.INSTANCE.createExchange(exchangeSpecification);
			ExchangeManager.getInstance("BitcoinCentral").setExchangeSpecification(exchangeSpecification);
			ExchangeManager.getInstance("BitcoinCentral").setTickerManagerClass(PollingTickerManager.class);
			log.info("Connecting to Bitcoin Central Exchange");
		return instance;
	}
}