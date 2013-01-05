/**
*
*/
package org.open.payment.alliance.isis.atp;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.net.Socket;

import org.joda.money.CurrencyUnit;

import com.xeiam.xchange.Currencies;
import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.ExchangeSpecification;
import com.xeiam.xchange.service.marketdata.polling.PollingMarketDataService;

/**
* @author Auberon
*
*/
public class PollingTickerManager extends TickerManager{
	
	private PollingMarketDataService marketData;
	private CurrencyUnit currency;

	public PollingTickerManager(CurrencyUnit currency) {
		super(currency);
		this.currency = currency;
		try {
			Exchange exchange = Application.getInstance().getExchange();
			marketData = exchange.getPollingMarketDataService();			
		} catch (Exception e) {
			e.printStackTrace();
		} 	
	}

	@Override
	public synchronized void run() {
		while(!quit){
			try {
				checkTick(marketData.getTicker(Currencies.BTC, currency.getCurrencyCode()));
				TimeUnit.SECONDS.sleep(10);
			} catch (com.xeiam.xchange.PacingViolationException | com.xeiam.xchange.HttpException e) {
				ExchangeSpecification exchangeSpecification = Application.getInstance().getExchange().getDefaultExchangeSpecification();
				Socket testSock = null;
				while (true) {
					try {
						log.warn("WARNING: Testing connection to exchange");
						testSock = new Socket(exchangeSpecification.getHost(),exchangeSpecification.getPort());
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
}