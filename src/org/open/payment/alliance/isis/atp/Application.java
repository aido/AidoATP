/**
* 
*/
package org.open.payment.alliance.isis.atp;

import java.io.Console;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.dto.account.AccountInfo;
import com.xeiam.xchange.service.trade.polling.PollingTradeService;

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
	private boolean useArbFlag;
	private Exchange exchange;
	private Console console;
	private Application() {
		log = LoggerFactory.getLogger(Application.class);
		params = new HashMap<String,String>();
		//true = simulate, false = live
		simModeFlag = true;
		//true = use arbitrage, false = do not use arbitage
		useArbFlag = true;
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
		}else {
			simModeFlag = showAgreement();
		}
		
		if(params.get("--use-arbitrage") != null) {
			if(params.get("--use-arbitrage").equalsIgnoreCase("true")) {
				System.out.println("Using arbitrage to decide some trades.");
				setArbMode(true);
			}else {
				setArbMode(false);
			}
		}
		
		exchange = IsisMtGoxExchange.getInstance();
		AccountManager.getInstance().refreshAccounts();
		if(useArbMode()){
			new Thread(ArbitrageEngine.getInstance()).start();
		}
		log.info("Isis ATP has started successfully");
		while(AccountManager.getInstance().isRunning()) {
			Thread.currentThread().yield();
		}

	}
	
	private boolean showAgreement() {
		System.out.print(this.getClass().getClassLoader().getResourceAsStream("license.txt"));
		if(params.get("--debug-live") != null) {
			if(params.get("--debug-live").equalsIgnoreCase("true")) {
				System.out.println("Entering live mode for real world debugging.");
				return false;
			}else {
				return true;
			}
		}
		String input = console.readLine();
		if(input.equalsIgnoreCase("I Agree")) {
			return false;
		}else {
			return true;
		}
	}
	private void interview() {
		
		PrintStream out = System.out;
		
		out.println("No config file could be found.");
		out.println("Beginning Interactive Mode");
		if(console == null) {
			out.println("No console could be found, exiting application.");
			System.exit(1);
		}
		out.println("Please answer all questions.\nDon't worry if you make a mistake you will have a chance to review before comitting.");
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
		
		out.print("Trading fee (eg 0.6% = 0.006): ");
		config.put("TradingFee", console.readLine());
		
		out.println("Which algorithm would you like to use? (1 or 2)");
		out.println("1: \"High Risk\"");
		out.println("2: \"Conservative\"");
		config.put("Algorithm", console.readLine());
		
		out.println("Interactive mode complete!");
		out.println("Please look carefully at the answers below and if they look correct press enter.");
		out.println("If there are any errors, please type NO");
		try {
			config.exportNode(out);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
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
					log.error("Bad Parameter: " + pair[0]+"\nParameters must be specified as \"-parameter=value\"");
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
	
	public boolean isSimMode() {
		return simModeFlag;
	}
	
	public boolean useArbMode() {
		return useArbFlag;
	}
	
	public AccountInfo getAccountInfo() {
		return AccountManager.getInstance().getAccountInfo();
	}

	public void setSimMode(boolean b) {
		this.simModeFlag = b;
	}
	
	public void setArbMode(boolean b) {
		this.useArbFlag = b;
	}

	public Exchange getExchange() {
		return exchange;
	}
}
