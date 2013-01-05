/**
*
*/
package org.open.payment.alliance.isis.atp;

import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.ExchangeFactory;
import com.xeiam.xchange.ExchangeSpecification;
import com.xeiam.xchange.mtgox.v1.MtGoxExchange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author Auberon
*
*/
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