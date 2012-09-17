/**
 * 
 */
package org.open.payment.alliance.isis.atp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

import org.joda.money.BigMoney;
import org.joda.time.DateTime;

import com.xeiam.xchange.Currencies;
import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.dto.marketdata.Ticker;
import com.xeiam.xchange.service.marketdata.polling.PollingMarketDataService;
import com.xeiam.xchange.service.trade.polling.PollingTradeService;

/**
 * @author steve
 *
 */
public class TickerManager implements Runnable{
	

	private PollingMarketDataService marketData;
	BlockingQueue<Ticker> tickerQ;
	private long currentVolume;
	private long lastVolume;
	private static ArrayList<ATPTicker> tickerCache;
	private Logger log;

	 TickerManager(Exchange mtgox, PollingTradeService tradeService) {
		log = Logger.getLogger(TickerManager.class.getSimpleName());
		try {
			tickerCache = loadMarketData();
			if(tickerCache == null) {
				tickerCache = new ArrayList<ATPTicker>();
			}
			marketData = mtgox.getPollingMarketDataService();
						
		} catch (Exception e) {
			e.printStackTrace();
		} 
		
	}

	@SuppressWarnings("unchecked")
	private ArrayList<ATPTicker> loadMarketData() {
		
		ArrayList<ATPTicker> data = new ArrayList<ATPTicker>();
		String path = System.getProperty("user.dir");
		if(path == null) {path = "";}
		
		File file = new File(path+"/market.dat");
		
		if(file.exists()) {
			
			log.info("Attempting to open market data file\n"+ path+"/market.dat");
						
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
		File file = new File(System.getProperty("user.dir")+"/market.dat");
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
	public void run() {
		
		while(true){
			try {
				Ticker tick = marketData.getTicker(Currencies.BTC, Currencies.USD);
				lastVolume = currentVolume;
				currentVolume = tick.getVolume();
				if(currentVolume != lastVolume) {
					synchronized(tickerCache) {
						tickerCache.add(new ATPTicker(tick));
					}
					//System.out.println(tick.toString());
				}
				saveMarketData();
				Thread.sleep(Constants.TENSECONDS);
			} catch (Exception e) {
				System.err.println("Caught unexpected exception, shutting down now!.\nDetails are listed below.");
				e.printStackTrace();
				System.exit(1);
			}
		}
		
	}


	public static ArrayList<ATPTicker> getMarketData(){
		
		synchronized(tickerCache) {
			ArrayList<ATPTicker> removeList = new ArrayList<ATPTicker>();
			DateTime now = new DateTime();
			for(ATPTicker tick : tickerCache){
				DateTime time = tick.getTimestamp();
				if(now.getMillis() - time.getMillis() > Constants.ONEHOUR) {
					removeList.add(tick);
				}
			}
			tickerCache.removeAll(removeList);
		}
		return tickerCache;
	}	
}
