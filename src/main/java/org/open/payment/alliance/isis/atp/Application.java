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

import java.io.Console;
import java.io.IOException;
import java.io.PrintStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author Auberon
*
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
	private Thread accountManagerThread;
	
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

		if(config.get("ApiKey", null) == null) {
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
		
		accountManagerThread = new Thread(AccountManager.getInstance());
		accountManagerThread.start();
		log.info("Isis ATP has started successfully");	
		
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
			accountManagerThread.join();
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
		out.print("Enter your API key: ");
		config.put("ApiKey",console.readLine());
		
		out.print("Enter your secret key: ");
		config.put("SecretKey", console.readLine());
		
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
		
		out.println("Enable Arbitrage trading engine");
		out.println("0: Disable");
		out.println("1: Enable");
		config.put("UseArbitrage", console.readLine());
		
		out.print("Minimum Profit to seek for Arbitrage (eg 10% = 0.10): ");
		config.put("TargetProfit", console.readLine());

		out.println("Enable Trend-following trading engine");
		out.println("0: Disable");
		out.println("1: Enable");
		config.put("UseTrend", console.readLine());

		out.print("Minimum ticker size for trending trade decisions: ");
		config.put("MinTickSize", console.readLine());

		out.print("Maximum ticker age for trending trade decisions (in minutes): ");		
		config.put("MaxTickAge", console.readLine());
		
		out.println("Use Advance/Decline Spread algorithm");
		out.println("0: No");
		out.println("1: Yes");
		config.put("UseADS", console.readLine());

		out.println("Use Simple Moving Average algorithm (SMA)");
		out.println("0: No");
		out.println("1: Yes");
		config.put("UseSMA", console.readLine());
		
		out.println("Use Exponential Moving Average algorithm (EMA)");
		out.println("0: No");
		out.println("1: Yes");
		config.put("UseEMA", console.readLine());
		
		out.print("Number of ticks used to calculate short Moving Average: ");
		config.put("ShortMATickSize", console.readLine());
		
		out.println("Use VWAP Cross algorithm");
		out.println("0: No");
		out.println("1: Yes");
		config.put("UseVWAPCross", console.readLine());

		out.println("Which risk algorithm would you like to use? (1 or 2)");
		out.println("1: High Risk");
		out.println("2: Conservative");
		config.put("Algorithm", console.readLine());
		
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
					log.error("ERROR: Bad Parameter: " + pair[0]+". Parameters must be specified as \"-parameter=value\"");
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