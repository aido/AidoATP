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
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.joda.money.CurrencyUnit;
import org.joda.time.DateTime;

import com.xeiam.xchange.dto.marketdata.Ticker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* Ticker Manager class.
*
* @author Aido
*/

public class TickerManager implements Runnable {

	private long currentVolume;
	private long lastVolume;
	private ArrayList<ATPTicker> tickerCache;
	private CurrencyUnit currency;
	public boolean quit;
	public Logger log;
	
	public TickerManager(CurrencyUnit currency) {
		log = LoggerFactory.getLogger(TickerManager.class);
		this.currency = currency;
		
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

	public ArrayList<ATPTicker> getMarketData(){
		
		synchronized(tickerCache) {
			ArrayList<ATPTicker> removeList = new ArrayList<ATPTicker>();
			DateTime now = new DateTime();
			for(ATPTicker tick : tickerCache){
				DateTime time = tick.getTimestamp();
				if(now.getMillis() - time.getMillis() > TimeUnit.MILLISECONDS.convert(Long.valueOf(Application.getInstance().getConfig("MaxTickAge")),TimeUnit.MINUTES)) {
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
					new Thread(ArbitrageEngine.getInstance()).start();
					ArbitrageEngine.getInstance().addTick(new ATPTicker(tick));
				}
				if (Application.getInstance().getTrendMode()) {
					new Thread(new TrendObserver(getMarketData())).start();
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