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

import java.io.Console;
import java.io.IOException;
import java.io.PrintStream;
import java.io.InputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* Application main class.
*
* @author Aido
*/

public class Application {

	private static Application instance = null;
	private static HashMap<String, String> params;
	private final Logger log;
	private Preferences config;
	private boolean simModeFlag;
	private boolean arbModeFlag;
	private boolean trendModeFlag;
	private Console console;
	private ThreadGroup exchangeManagerThreadGroup;
	private HashMap<String, Thread> exchangeManagers;

	private Application() {
		log = LoggerFactory.getLogger(Application.class);
		params = new HashMap<String,String>();
		//true = simulate, false = live
		simModeFlag = true;
		//true = use arbitrage, false = do not use arbitage
		arbModeFlag = true;
		//true = use trend following trades, false = do not use trend following trades
		trendModeFlag = true;
		console = System.console();
	}

	public static Application getInstance() {
		if(instance == null) {
			instance = new Application();
		}
		return instance;
	}
	/**
	* @param args
	*/
	public static void main(String[] args) {
		Application app = getInstance();
		app.start(args);
	}

	public void start(String[] args){
	
		List<String> exchangeArray = new ArrayList<String>();
		parseArgs(args);

		config = Preferences.userNodeForPackage(this.getClass());

		if(params.containsKey("--clear-config")) {
			log.info("Clearing out all configuration data.");
			try {
				config.clear();
				config.sync();
			} catch (BackingStoreException e) {
				e.printStackTrace();
			}
		}

		if(config.get("MtGoxApiKey", null) == null && config.get("BTC-eApiKey", null) == null && config.get("BitstampUserName", null) == null) {
			interview();
		}

		if(params.get("--simulation-mode") != null){
			setSimMode(Boolean.valueOf(params.get("--simulation-mode")));
		}else if (getSimMode()) {
			showAgreement();
		}

		setArbMode(getConfig("UseArbitrage").equals("1"));

		if(params.get("--use-arbitrage") != null){
			setArbMode(Boolean.valueOf(params.get("--use-arbitrage")));
		}

		setTrendMode(getConfig("UseTrend").equals("1"));

		if(params.get("--use-trend") != null){
			setTrendMode(Boolean.valueOf(params.get("--use-trend")));
		}

		exchangeManagers = new HashMap<String, Thread>();
		exchangeManagerThreadGroup = new ThreadGroup("ExchangeManagers");
		for (String exchange : ExchangeManager.getExchangesHashMap().keySet()) {
			if (getConfig("Use" + exchange).equals("1")) {
				exchangeManagers.put(exchange, new Thread(exchangeManagerThreadGroup,ExchangeManager.getInstance(exchange)));
				exchangeManagers.get(exchange).start();
				exchangeArray.add(exchange);
			}
		}
		log.info("Aido ATP has started successfully");

		if(getSimMode()){
			log.info("Entering simulation mode. Trades will not be executed.");
		}

		if(getTrendMode()){
			log.info("Using trend following to decide some trades.");
		}

		if(getArbMode()){
			log.info("Using arbitrage to decide some trades.");
		}

		try {
			for (String exchange : exchangeArray)
				exchangeManagers.get(exchange).join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void showAgreement() {
		InputStream disclaimer = this.getClass().getClassLoader().getResourceAsStream("disclaimer.txt");

		byte[] buf = new byte[2048];
		long total = 0;
		int len = 0;
		try {
			while (-1 != (len = disclaimer.read(buf))) {
				System.out.write(buf, 0, len);
				total += len;
			}
		} catch (IOException e) {
			throw new RuntimeException("Error displaying disclaimer", e);
		}
		System.out.print("Please type your response : ");

		String input = console.readLine();
		if(input.equalsIgnoreCase("I Agree")) {
			setSimMode(false);
		}else {
			setSimMode(true);
		}
	}

	private void interview() {

		PrintStream out = System.out;

		log.info("No config file could be found.");
		log.info("Beginning Interactive Mode");
		if(console == null) {
			log.error("ERROR: No console could be found, exiting application.");
			System.exit(1);
		}
		out.println("\nPlease answer all questions.\nDon't worry if you make a mistake you will have a chance to review before comitting.");

		for (String exchange : ExchangeManager.getExchangesHashMap().keySet()) {
			out.print("Use " + exchange + " exchange (y/n): ");
			if(console.readLine().equalsIgnoreCase("Y") ) {
				if (exchange.equals("Bitstamp") || exchange.equals("BitcoinCentral") || exchange.equals("CampBX")) {
					out.print("Enter your " + exchange + " Username: ");
					config.put(exchange + "UserName",console.readLine());
					out.print("Enter your " + exchange + " Password: ");
					config.put(exchange + "Password", console.readLine());
				} else {
					out.print("Enter your " + exchange + " API key: ");
					config.put(exchange + "ApiKey",console.readLine());
					out.print("Enter your " + exchange + " secret key: ");
					config.put(exchange + "SecretKey", console.readLine());
				}
				config.put("Use"+ exchange, "1");
			} else {
				config.put("Use"+ exchange, "0");
			}
		}

		out.print("ISO Code for Prefered Currency (i.e. USD, GBP, JPY, EUR etc): ");
		config.put("LocalCurrency", console.readLine());

		out.print("Maximum number of bitcoins to trade in a single order: ");
		config.put("MaxBTC", console.readLine());

		out.print("Minimum number of bitcoins to trade in a single order: ");
		config.put("MinBTC", console.readLine());

		out.print("Maximum amount of local currency to trade in a single order: ");
		config.put("MaxLocal", console.readLine());

		out.print("Minimum amount of local currency to trade in a single order: ");
		config.put("MinLocal", console.readLine());

		out.print("Overall maximum loss tolerance (eg 25% = 0.25): ");
		config.put("MaxLoss", console.readLine());

		out.print("Enable Arbitrage trading engine (y/n): ");
		if(console.readLine().equalsIgnoreCase("Y") ) {
			config.put("UseArbitrage", "1");
		} else {
			config.put("UseArbitrage", "0");
		}

		out.print("Minimum Profit to seek for Arbitrage (eg 10% = 0.10): ");
		config.put("TargetProfit", console.readLine());

		out.print("Enable Trend-following trading engine (y/n): ");
		if(console.readLine().equalsIgnoreCase("Y") ) {
			config.put("UseTrend", "1");
		} else {
			config.put("UseTrend", "0");
		}

		out.print("Polling Interval (in seconds): ");
		config.put("PollingInterval", console.readLine());

		out.print("Minimum ticker size for trending trade decisions: ");
		config.put("MinTickSize", console.readLine());

		out.print("Maximum ticker age for trending trade decisions (in minutes): ");
		config.put("MaxTickAge", console.readLine());

		out.print("Number of ticks used to calculate short Moving Average: ");
		config.put("ShortMATickSize", console.readLine());

		out.print("Number of ticks used to calculate short Moving Average Convergence-Divergence: ");
		config.put("ShortMACDTickSize", console.readLine());

		out.print("Number of ticks used to calculate long Moving Average Convergence-Divergence: ");
		config.put("LongMACDTickSize", console.readLine());

		out.print("Number of MACD values used to calculate MACD Signal Line: ");
		config.put("SigLineMACDSize", console.readLine());

		out.print("Bid Logic: ");
		config.put("BidLogic", console.readLine());

		out.print("Ask Logic: ");
		config.put("AskLogic", console.readLine());

		out.println("Which risk algorithm would you like to use? (1 - 3)");
		out.println("1: Conservative Risk");
		out.println("2: High Risk");
		out.println("3: Maximum Risk");
		config.put("RiskAlgorithm", console.readLine());

		out.print("Trading fee (eg 0.6% = 0.006): ");
		config.put("TradingFee", console.readLine());

		try {
			config.exportNode(out);
		} catch (Exception e) {
			e.printStackTrace();
		}

		out.println("Interactive mode complete!");
		out.println("Please look carefully at the answers above and if they look correct press enter.");
		out.println("If there are any errors, please type NO");
		String answer = console.readLine();

		if(answer == null || answer.isEmpty()){
			try {
				config.sync();
			} catch (BackingStoreException e) {
				e.printStackTrace();
			}
		}else{
			interview();
		}

	}

	private void parseArgs(String[] args) {
		for(String arg : args) {
			String[] pair = arg.split("=");
			if(pair != null) {
				try {
					params.put(pair[0], pair[1]);
				}catch( Exception ex){
					log.error("ERROR: Bad Parameter: {}. Parameters must be specified as \"-parameter=value\"",pair[0]);
					ex.printStackTrace();
					System.exit(1);
				}
			}
		}
	}

	public String getParam(String key){
		return params.get(key);
	}

	public String getConfig(String key) {
		return config.get(key,null);
	}

	public boolean getSimMode() {
		return simModeFlag;
	}

	public void setSimMode(boolean b) {
		this.simModeFlag = b;
	}

	public boolean getArbMode() {
		return arbModeFlag;
	}

	public void setArbMode(boolean b) {
		this.arbModeFlag = b;
	}

	public boolean getTrendMode() {
		return trendModeFlag;
	}

	public void setTrendMode(boolean b) {
		this.trendModeFlag = b;
	}
}