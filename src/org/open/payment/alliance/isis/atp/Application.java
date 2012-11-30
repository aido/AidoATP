/**
* 
*/
package org.open.payment.alliance.isis.atp;

import java.io.Console;
import java.io.IOException;
import java.io.PrintStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.dto.account.AccountInfo;

import org.joda.money.CurrencyUnit;

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
	private Exchange exchange;
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
			if(params.get("--simulation-mode").equalsIgnoreCase("true")) {
				setSimMode(true);
			}else {
				setSimMode(false);
			}
		}else if (getSimMode()) {
			showAgreement();
		}

		if(params.get("--use-arbitrage") != null){
			if(params.get("--use-arbitrage").equalsIgnoreCase("true")) {
				setArbMode(true);
			}else {
				setArbMode(false);
			}
		}
	
		if(params.get("--use-trend") != null){
			if(params.get("--use-trend").equalsIgnoreCase("true")) {
				setTrendMode(true);
			}else {
				setTrendMode(false);
			}
		}

		exchange = IsisMtGoxExchange.getInstance();
		
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
		InputStream license = this.getClass().getClassLoader().getResourceAsStream("license.txt");

		byte[] buf = new byte[2048];
		long total = 0;
		int len = 0;
		try {
			while (-1 != (len = license.read(buf))) {
				System.out.write(buf, 0, len);
				total += len;
			}
		} catch (IOException e) {
			throw new RuntimeException("Error displaying license agreement", e);
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
		
		out.print("Minimum Profit to seek for Arbitrage (eg 10% = 0.10): ");
		config.put("TargetProfit", console.readLine());

		out.print("Minimum ticker size for trending trade decisions: ");
		config.put("minTickSize", console.readLine());

		out.print("Maximum ticker age for trending trade decisions (in minutes): ");
		config.put("maxTickAge", console.readLine());
		
		out.print("Trading fee (eg 0.6% = 0.006): ");
		config.put("TradingFee", console.readLine());
		
		out.println("Which algorithm would you like to use? (1 or 2)");
		out.println("1: \"High Risk\"");
		out.println("2: \"Conservative\"");
		config.put("Algorithm", console.readLine());
		
		try {
			config.exportNode(out);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		out.println("Interactive mode complete!");
		out.println("Please look carefully at the answers below and if they look correct press enter.");
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
	public void stop() {
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
	
	public boolean getArbMode() {
		return arbModeFlag;
	}
	
	public boolean getTrendMode() {
		return trendModeFlag;
	}
	
	public AccountInfo getAccountInfo() {
		return AccountManager.getInstance().getAccountInfo();
	}

	public void setSimMode(boolean b) {
		this.simModeFlag = b;
	}
	
	public void setArbMode(boolean b) {
		this.arbModeFlag = b;
	}

	public void setTrendMode(boolean b) {
		this.trendModeFlag = b;
	}

	public Exchange getExchange() {
		return exchange;
	}
}