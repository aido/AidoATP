package org.open.payment.alliance.isis.atp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.net.Socket;

import org.joda.money.BigMoney;
import org.joda.money.CurrencyUnit;

import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.ExchangeSpecification;
import com.xeiam.xchange.dto.account.AccountInfo;
import com.xeiam.xchange.dto.trade.Wallet;
import com.xeiam.xchange.service.account.polling.PollingAccountService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccountManager implements Runnable {

	private static AccountManager instance = null;
	private AccountInfo accountInfo;	

	private HashMap<CurrencyUnit, ArrayList<BigMoney>> books;//We only look at first and last right now, but it would be handy to have changes over time as well.
	private HashMap<CurrencyUnit, TickerManager> tickerTracker;
	private ThreadGroup tickerThreadGroup;
	
	private static Logger log;
	
	private Exchange exchange;
	private PollingAccountService accountService;
	private List<Wallet> wallets;
	
	public static AccountManager getInstance() {
		if(instance == null) {
			instance = new AccountManager();
		}
		return instance;
	}
	
	private AccountManager(){
		
		try {	
			tickerTracker = new HashMap<CurrencyUnit, TickerManager>();
			tickerThreadGroup = new ThreadGroup("Tickers");
			
			log = LoggerFactory.getLogger(AccountManager.class);
			books = new HashMap<CurrencyUnit, ArrayList<BigMoney>>();
			
			exchange = Application.getInstance().getExchange();
			
			// Interested in the private account functionality (authentication)
			accountService = exchange.getPollingAccountService();
			
			// Get the account information
			accountInfo = accountService.getAccountInfo();
			log.info("AccountInfo as String: " + accountInfo.toString());
			refreshAccounts();
	
			for(Wallet wallet : wallets) {
				CurrencyUnit currency = wallet.getBalance().getCurrencyUnit();
				if(currency.getCode().equals("BTC")) {
					continue;
				}
//				tickerTracker.put(currency, new PollingTickerManager(currency));
				tickerTracker.put(currency, new StreamingTickerManager(currency));
				new Thread(tickerThreadGroup,tickerTracker.get(currency),currency.getCode()).start();
			}
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
			log.error("ERROR: Caught unexpected exception, shutting down now!.Details are listed below.");
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	@Override
	public synchronized void run() {
		refreshAccounts();
	}

	public BigMoney getBalance(CurrencyUnit currency) throws WalletNotFoundException{
		refreshAccounts();
		wallets = accountInfo.getWallets();
		
		for(Wallet wallet : wallets){
			BigMoney balance = wallet.getBalance();
			CurrencyUnit unit = balance.getCurrencyUnit();
			
			if(unit.equals(currency)){
				return balance;
			}
		}
		log.error("ERROR: Could not find a wallet for the currency "+currency+". Exiting now!");
		throw new WalletNotFoundException();
	}
	
	public synchronized void refreshAccounts() {
		accountInfo = accountService.getAccountInfo();
		updateBooks();
		ProfitLossAgent.getInstance().updateBalances(accountInfo.getWallets());
	}

	private void updateBooks() {
		wallets = accountInfo.getWallets();
		for(Wallet wallet : wallets){
			CurrencyUnit currency = wallet.getBalance().getCurrencyUnit();
			
			//Do we have a new currency in our wallet?
			if(books.get(currency) == null){
				//Make some space for it.
				books.put(currency,new ArrayList<BigMoney>());
			}
			
			ArrayList<BigMoney> ledger = books.get(currency);
			ledger.add(wallet.getBalance());
		}
	}

	public AccountInfo getAccountInfo() {
		return accountInfo;
	}
}