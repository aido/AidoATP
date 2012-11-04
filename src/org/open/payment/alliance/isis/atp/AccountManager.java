package org.open.payment.alliance.isis.atp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.joda.money.BigMoney;
import org.joda.money.CurrencyUnit;

import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.dto.account.AccountInfo;
import com.xeiam.xchange.dto.trade.Wallet;
import com.xeiam.xchange.service.account.polling.PollingAccountService;

public class AccountManager {

	private static AccountManager instance = null;
	private AccountInfo accountInfo;
	

	private HashMap<CurrencyUnit, ArrayList<BigMoney>> books;//We only look at first and last right now, but it would be handy to have changes over time as well.
	private HashMap<CurrencyUnit, PLModel> PL; //PL is Profit/Loss and is per currency unit
	private HashMap<CurrencyUnit, CurrencyManager> currencyTracker;
	
	
	private Logger log;
	
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
		
		currencyTracker = new HashMap<CurrencyUnit, CurrencyManager>();
		log = Logger.getLogger(AccountManager.class.getSimpleName());
		books = new HashMap<CurrencyUnit, ArrayList<BigMoney>>();
		PL = new HashMap<CurrencyUnit, PLModel>();
		
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
	    	currencyTracker.put(currency, new CurrencyManager(currency));
	    }
	    
	}
	
	
	
	
	public BigMoney getBalance(CurrencyUnit currency) throws WalletNotFoundException{
		refreshAccounts();
		wallets = accountInfo.getWallets();
		//log.info("You have wallets for "+wallets.size()+" currencies.");
		for(Wallet wallet : wallets){
			BigMoney balance = wallet.getBalance();
			CurrencyUnit unit = balance.getCurrencyUnit();
			
			if(unit.equals(currency)){
				return balance;
			}
		
		}
		log.severe("Could not find a wallet for the currency "+currency+"\nExiting now!");
		throw new WalletNotFoundException();
	}
	
	public synchronized void refreshAccounts() {
		accountInfo = accountService.getAccountInfo();
		updateBooks();
		calculatePL();
	}
	
	private synchronized void calculatePL() {
		synchronized(books) {
			synchronized(PL) {
				PL.clear();
				for(CurrencyUnit currency : books.keySet()) {
					ArrayList<BigMoney> ledger = books.get(currency);
					BigMoney beginBal = ledger.get(0);
					BigMoney endBal = ledger.get(ledger.size()-1);
					BigDecimal profitAsPercent;
					BigMoney profitAsCurrency = endBal.minus(beginBal);
					
					if(beginBal.getAmount().compareTo(BigDecimal.ZERO) !=0 && endBal.getAmount().compareTo(BigDecimal.ZERO) != 0) {
						profitAsPercent = profitAsCurrency.getAmount().divide(beginBal.getAmount(),RoundingMode.HALF_EVEN);
					}else {
						profitAsPercent = BigDecimal.ZERO;
					}
					
					profitAsPercent = profitAsPercent.multiply(new BigDecimal("100"));
					PL.put(currency, new PLModel(profitAsCurrency, profitAsPercent));
				}
			}
		}
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
	
	public PLModel getPLFor(CurrencyUnit currency){
		return PL.get(currency);
	}

	public AccountInfo getAccountInfo() {
		return accountInfo;
	}

	public boolean isRunning() {
		boolean running = true;
		for(CurrencyUnit currency : currencyTracker.keySet()) {
			CurrencyManager manager = currencyTracker.get(currency);
			running = manager.isRunning();
			if(running == false) {
				break;
			}
		}
		return running;
	}

	public ATPTicker getLastTick(CurrencyUnit baseCurrency) {
		
		return currencyTracker.get(baseCurrency).getTickerManager().getLastTick();
	}
}
