package org.open.payment.alliance.isis.atp;

import org.joda.money.CurrencyUnit;

public class CurrencyManager implements Runnable{
	private CurrencyUnit currency;
	private PollingTickerManager tickerManager;
	private Thread managerThread;	
	
	public CurrencyManager(CurrencyUnit currency) {
		this.currency = currency;
		tickerManager = new PollingTickerManager(currency);
		managerThread = new Thread(tickerManager);
		managerThread.start();
	}
	public CurrencyUnit getCurrency() {
		return currency;
	}
	public void setCurrency(CurrencyUnit currency) {
		this.currency = currency;
	}
	public PollingTickerManager getTickerManager() {
		return tickerManager;
	}

	@Override
	public void run() {
		managerThread.start();
	}
	
	public void stop() {
		tickerManager.stop();
	}
	public boolean isRunning() {
		return managerThread.isAlive();
	}
}
