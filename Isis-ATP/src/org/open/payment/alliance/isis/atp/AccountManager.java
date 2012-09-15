package org.open.payment.alliance.isis.atp;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.joda.money.BigMoney;
import org.joda.money.CurrencyUnit;

import com.xeiam.xchange.dto.trade.AccountInfo;
import com.xeiam.xchange.dto.trade.Wallet;

public class AccountManager {

	private static AccountManager instance = null;
	private AccountInfo accountInfo;
	

	private HashMap<CurrencyUnit, ArrayList<BigMoney>> books;//We only look at first and last right now, but it would be handy to have changes over time as well.
	private HashMap<CurrencyUnit, PLModel> PL; //PL is Profit/Loss and is per currency unit
	
	private Logger log;
	
	private AccountManager(){
		log = Logger.getLogger(AccountManager.class.getSimpleName());
		books = new HashMap<CurrencyUnit, ArrayList<BigMoney>>();
		PL = new HashMap<CurrencyUnit, PLModel>();
		refreshAccount();
	}
	
	public static AccountManager getInstance() {
		if(instance == null) {
			instance = new AccountManager();
		}
		return instance;
	}
	
	public BigMoney getBalance(CurrencyUnit currency) throws WalletNotFoundException{
		refreshAccount();
		List<Wallet> wallets = accountInfo.getWallets();
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
	
	public synchronized void refreshAccount() {
		accountInfo = Application.getInstance().getAccountInfo();
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
					
					BigMoney profitAsCurrency = endBal.minus(beginBal);
					BigDecimal profitAsPercent = beginBal.getAmount().divide(endBal.getAmount());
					profitAsPercent = profitAsPercent.multiply(new BigDecimal("100"));
					PL.put(currency, new PLModel(profitAsCurrency, profitAsPercent));
				}
			}
		}
	}
	private void updateBooks() {
		List<Wallet> wallets = accountInfo.getWallets();
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
}
