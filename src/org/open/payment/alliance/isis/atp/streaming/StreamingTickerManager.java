/**
* 
*/
package org.open.payment.alliance.isis.atp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.net.Socket;

import org.joda.money.CurrencyUnit;
import org.joda.time.DateTime;

import com.xeiam.xchange.Currencies;
import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.ExchangeSpecification;
import com.xeiam.xchange.dto.marketdata.Ticker;
import com.xeiam.xchange.service.marketdata.streaming.StreamingMarketDataService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author Auberon
*
*/
public class StreamingTickerManager implements Runnable{
	
	private StreamingMarketDataService marketData;
	private long currentVolume;
	private long lastVolume;
	private Logger log;
	private ArrayList<ATPTicker> tickerCache;
	private CurrencyUnit currency;
	private boolean quit;
	private BlockingQueue<Ticker> tickerQueue;
					
	StreamingTickerManager(CurrencyUnit currency) {
		log = LoggerFactory.getLogger(StreamingTickerManager.class);
		this.currency = currency;
		quit = false;
		try {
			tickerCache = loadMarketData();
			if(tickerCache == null) {
				tickerCache = new ArrayList<ATPTicker>();
			}

			Exchange exchange = com.xeiam.xchange.mtgox.v1.MtGoxExchange.newInstance();
			marketData = exchange.getStreamingMarketDataService();
			tickerQueue = marketData.requestTicker(Currencies.BTC, currency.getCurrencyCode());				
		} catch (Exception e) {
			e.printStackTrace();
		} 	
	}

	@SuppressWarnings("unchecked")
	private synchronized ArrayList<ATPTicker> loadMarketData() {
		
		ArrayList<ATPTicker> data = new ArrayList<ATPTicker>();
		String path = System.getProperty("user.dir");
		if(path == null) {path = "";}
		
		File file = new File(path+"/"+currency.getCurrencyCode()+".dat");
		
		if(file.exists()) {
			
			log.info("Attempting to open market data file "+path+"/"+currency.getCurrencyCode()+".dat");
			
			try {
				ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
				data = (ArrayList<ATPTicker>) ois.readObject();
				ois.close();
			} catch (Exception e) {	
				e.printStackTrace();
			}
		}else {
			log.info("File "+file+" does not exist yet.  Either this is the first run or the market data file did not save properly last time.");
		}
		
		return data;
	}

	private synchronized void saveMarketData() {
		File file = new File(System.getProperty("user.dir")+"/"+currency.getCurrencyCode()+".dat");
		ObjectOutputStream oos;
		try {
			oos = new ObjectOutputStream(new FileOutputStream(file));
			synchronized(tickerCache) {
				oos.writeObject(tickerCache);
			}
			oos.flush();
			oos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public synchronized void run() {

		while(!quit){
			try {
				Ticker tick = tickerQueue.take();
				currentVolume = tick.getVolume().longValue();
				if(currentVolume != lastVolume) {
					synchronized(tickerCache) {
						if (Application.getInstance().getArbMode()) {
							new Thread(ArbitrageEngine.getInstance()).start();
							ArbitrageEngine.getInstance().addTick(new ATPTicker(tick));
						}
						if (Application.getInstance().getTrendMode()) {
							new Thread(new TrendObserver(this)).start();
						}
						tickerCache.add(new ATPTicker(tick));
						lastVolume = currentVolume;
					}
				}
				saveMarketData();
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
							Thread.currentThread().sleep(Constants.ONEMINUTE);
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


	public ArrayList<ATPTicker> getMarketData(){
		
		synchronized(tickerCache) {
			ArrayList<ATPTicker> removeList = new ArrayList<ATPTicker>();
			DateTime now = new DateTime();
			for(ATPTicker tick : tickerCache){
				DateTime time = tick.getTimestamp();
				if(now.getMillis() - time.getMillis() > Integer.valueOf(Application.getInstance().getConfig("maxTickAge")) * Constants.ONEMINUTE ) {
					removeList.add(tick);
				}
			}
			tickerCache.removeAll(removeList);
		}
		return tickerCache;
	}

	public void stop() {
		quit = true;
	}

	public CurrencyUnit getCurrency() {
		return currency;
	}

}
