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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.net.Socket;

import org.joda.money.BigMoney;
import org.joda.money.CurrencyUnit;

import com.xeiam.xchange.dto.account.AccountInfo;
import com.xeiam.xchange.dto.trade.Wallet;
import com.xeiam.xchange.service.account.polling.PollingAccountService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* Account manager class.
*
* @author Aido
*/

public class AccountManager implements Runnable {

	private static AccountManager instance = null;
	private AccountInfo accountInfo;
	private HashMap<CurrencyUnit, ArrayList<BigMoney>> books;//We only look at first and last right now, but it would be handy to have changes over time as well.
	private HashMap<CurrencyUnit, TickerManager> tickerTracker;
	private ThreadGroup tickerThreadGroup;
	private static Logger log;
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

			new Thread(ExchangeManager.getInstance()).start();
			
			while ( ExchangeManager.getInstance().getExchange() == null )
			{
				try {
					log.info("Waiting to connect.");
					TimeUnit.SECONDS.sleep(2);
				} catch (InterruptedException e2) {
						e2.printStackTrace();
				}
			}			
			
			// Interested in the private account functionality (authentication)
			accountService = ExchangeManager.getInstance().getExchange().getPollingAccountService();
			
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
			Socket testSock = null;
			while (true) {
				try {
					log.warn("WARNING: Testing connection to exchange");
					testSock = new Socket(ExchangeManager.getInstance().getHost(),ExchangeManager.getInstance().getPort());
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