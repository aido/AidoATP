package org.open.payment.alliance.isis.atp;

import org.joda.money.CurrencyUnit;

public class CurrencyManager implements Runnable{
	private CurrencyUnit currency;
	private TickerManager tickerManager;
	private TrendObserver trendObserver;
	private Thread managerThread;
	private Thread observerThread;
	
	
	public CurrencyManager(CurrencyUnit currency) {
		this.currency = currency;
		tickerManager = new TickerManager(currency);
		trendObserver = new TrendObserver(tickerManager);
		managerThread = new Thread(tickerManager);
		observerThread = new Thread(trendObserver);
		managerThread.start();
		observerThread.start();
	}
	public CurrencyUnit getCurrency() {
		return currency;
	}
	public void setCurrency(CurrencyUnit currency) {
		this.currency = currency;
	}
	public TickerManager getTickerManager() {
		return tickerManager;
	}

	public TrendObserver getTrendObserver() {
		return trendObserver;
	}
	@Override
	public void run() {
		managerThread.start();
		observerThread.start();
	}
	
	public void stop() {
		tickerManager.stop();
		trendObserver.stop();
	}
	public boolean isRunning() {
		
		return managerThread.isAlive() && observerThread.isAlive();
	}
}
