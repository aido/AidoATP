/**
 * 
 */
package org.open.payment.alliance.isis.atp;

import java.util.logging.Logger;

import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.ExchangeFactory;
import com.xeiam.xchange.ExchangeSpecification;
import com.xeiam.xchange.mtgox.v1.MtGoxExchange;

/**
 * @author Auberon
 *
 */
public class IsisMtGoxExchange extends MtGoxExchange {
	
	private static Exchange instance = null;
	private static Logger logger = Logger.getLogger(IsisMtGoxExchange.class.getName());
	private static ExchangeSpecification exchangeSpecification;
	public static Exchange getInstance() {
		if(instance == null) {
			exchangeSpecification = new ExchangeSpecification("com.xeiam.xchange.mtgox.v1.MtGoxExchange");

			String apiKey = Application.getInstance().getConfig("ApiKey");
			String secretKey= Application.getInstance().getConfig("SecretKey");
			
			logger.config(apiKey);
			logger.config(secretKey);
			
			exchangeSpecification.setApiKey(apiKey);
		    exchangeSpecification.setSecretKey(secretKey);
		    exchangeSpecification.setUri("https://mtgox.com");
		    exchangeSpecification.setVersion("1");
		    instance = ExchangeFactory.INSTANCE.createExchange(exchangeSpecification);
		}
		return instance;
	}

}
