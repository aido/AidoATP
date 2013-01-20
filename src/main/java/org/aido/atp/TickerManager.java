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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.joda.money.CurrencyUnit;

import com.xeiam.xchange.dto.marketdata.Ticker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* Ticker Manager class.
*
* @author Aido
*/

public class TickerManager implements Runnable {

	private static HashMap<Pair, TickerManager> instances = new HashMap<Pair, TickerManager>();
	private long currentVolume;
	private long lastVolume;
	private ArrayList<ATPTicker> tickerCache;
	private String exchangeName;
	private String fileName;
	private ThreadGroup tickerThreadGroup;
	public boolean quit;
	public Logger log;
	
	public static TickerManager getInstance(String exchangeName, CurrencyUnit currency) {
		Pair exchangeCurrency = new Pair(exchangeName, currency);
		if(instances.get(exchangeCurrency) == null)
			if (exchangeName.equals("MtGox")) {
				instances.put(exchangeCurrency,new PollingTickerManager(currency,exchangeName));
			} else if (exchangeName.equals("BTC-e")) {
				instances.put(exchangeCurrency,new PollingTickerManager(currency,exchangeName));
			}
		return instances.get(exchangeCurrency);
	}
	
	public TickerManager(CurrencyUnit currency, String exchangeName) {
		log = LoggerFactory.getLogger(TickerManager.class);
		this.exchangeName = exchangeName;
		this.fileName = exchangeName+"_"+currency.getCurrencyCode()+".dat";

		quit = false;
		try {
			tickerCache = loadMarketData();
			if(tickerCache == null) {
				tickerCache = new ArrayList<ATPTicker>();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		while(!quit){
			getTick();
		}
	}

	public void getTick() {
	}
	
	@SuppressWarnings("unchecked")
	private synchronized ArrayList<ATPTicker> loadMarketData() {
		
		ArrayList<ATPTicker> data = new ArrayList<ATPTicker>();
		String path = System.getProperty("user.dir");
		if(path == null) {path = "";}
		
		File file = new File(path+"/"+fileName);
		
		if(file.exists()) {
			
			log.info("Attempting to open market data file "+path+"/"+fileName);
			
			try {
				ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
				data = (ArrayList<ATPTicker>) ois.readObject();
				ois.close();
			} catch (Exception e) {	
				e.printStackTrace();
			}
		}else {
			log.info("File "+file+" does not exist yet. Either this is the first run or the market data file did not save properly last time.");
		}
		
		return data;
	}

	private synchronized void saveMarketData() {
		File file = new File(System.getProperty("user.dir")+"/"+fileName);
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

	public ArrayList<ATPTicker> getMarketData(){
		
		synchronized(tickerCache) {
			ArrayList<ATPTicker> removeList = new ArrayList<ATPTicker>();
			Date now = new Date();
			for(ATPTicker tick : tickerCache){
				Date time = tick.getTimestamp();
				if(now.getTime() - time.getTime() > TimeUnit.MILLISECONDS.convert(Long.valueOf(Application.getInstance().getConfig("MaxTickAge")),TimeUnit.MINUTES)) {
					removeList.add(tick);
				}
			}
			tickerCache.removeAll(removeList);
		}
		return tickerCache;
	}

	public void checkTick(Ticker tick) {
		currentVolume = tick.getVolume().longValue();
		if(currentVolume != lastVolume) {
			synchronized(tickerCache) {
				tickerCache.add(new ATPTicker(tick));
				if (Application.getInstance().getArbMode()) {
					new Thread(ArbitrageEngine.getInstance(exchangeName)).start();
					ArbitrageEngine.getInstance(exchangeName).addTick(new ATPTicker(tick));
				}
				if (Application.getInstance().getTrendMode()) {
					new Thread(new TrendObserver(exchangeName,getMarketData())).start();
				}
				ProfitLossAgent.getInstance().updateRates(tick.getAsk());
				lastVolume = currentVolume;
			}
		}
		saveMarketData();	
	}
	
	public void stop() {
		quit = true;
	}
}