package org.open.payment.alliance.isis.atp;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.logging.Logger;

import org.joda.money.BigMoney;
import org.joda.money.CurrencyUnit;

import com.xeiam.xchange.dto.Order.OrderType;
import com.xeiam.xchange.dto.trade.MarketOrder;
import com.xeiam.xchange.service.trade.polling.PollingTradeService;

public class ArbitrageEngine implements Runnable {
	
	private static ArbitrageEngine instance = null;
	private boolean quit;
	private HashMap<CurrencyUnit, Double> askMap,bidMap;
	private CurrencyUnit baseCurrency;
	private double factor;
	private Logger log;
	
	
	private ArbitrageEngine() {
		quit= false;
		log = Logger.getLogger(ArbitrageEngine.class.getSimpleName());
		askMap = new HashMap<CurrencyUnit, Double>();
		bidMap = new HashMap<CurrencyUnit, Double>();
		baseCurrency = CurrencyUnit.getInstance( Application.getInstance().getConfig("LocalCurrency"));
	}

	public static synchronized ArbitrageEngine getInstance() {
		if(instance == null) {
			instance = new ArbitrageEngine();
		}
		return instance;
	}
	@Override
	public void run() {
		
		while(!quit) {
			Application.getInstance().setSimMode(true);//Lock out the other engine from trade execution while we arbitrage, any opportunities will still be there later.
			Pair<CurrencyUnit, Double> highestBid = null;
			try {
				highestBid = getHighestBid();
			} catch (WalletNotFoundException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
			Pair<CurrencyUnit, Double> lowestAsk = null;
			try {
				lowestAsk = getLowestAsk();
			} catch (WalletNotFoundException e1) {
				e1.printStackTrace();
				quit = true;//We should never, ever be able to get here, period.
			}
				
			Double fee = new Double(Application.getInstance().getConfig("TradingFee"));
			Double targetProfit = new Double(Application.getInstance().getConfig("TargetProfit"));
			
			//We buy from the lowestAsk & sell to the highestBid;
			double profit = highestBid.getSecond() - lowestAsk.getSecond();
			double profitAfterFee = profit - (fee *2);
			String profitToDisplay = NumberFormat.getPercentInstance().format(profitAfterFee);
			if(profitAfterFee > targetProfit){
				log.info("Arbitrage Engine has detected an after fee profit opportunity of %"+profitAfterFee
						+" on currency pair "+lowestAsk.getFirst()+"/"+highestBid.getFirst());
				
				log.info("\n***Conversion Factors***\nHighest Bid: "+highestBid.toString()+"\nLowest Ask: "+lowestAsk.toString()+"\n");
				
				try {
					executeTrade(lowestAsk,highestBid);
				} catch (WalletNotFoundException e) {
					e.printStackTrace();
				}
				
			}else {
				log.info("Arbitrage Engine cannot find a profitable opportunity at this time.");
			}
			Application.getInstance().setSimMode(false);
			try {
				Thread.sleep(Constants.TENSECONDS);
			} catch (InterruptedException e) {
				
				e.printStackTrace();
				quit = true;
			}
		}
	}

	
	/**
	 * Create 2 orders, a buy & a sell
	 * @param from
	 * @param to
	 * @throws WalletNotFoundException 
	 */
	private synchronized void executeTrade(Pair<CurrencyUnit,Double> from, Pair<CurrencyUnit, Double> to) throws WalletNotFoundException {
		
		CurrencyUnit fromCur = from.getFirst();
		CurrencyUnit toCur = to.getFirst();
		
		/*
		double baseValue = AccountManager.getInstance().getLastTick(baseCurrency).getLast().getAmount().doubleValue();
		
		BigMoney fromBalance = AccountManager.getInstance().getBalance(fromCur);
		BigMoney toBalance = AccountManager.getInstance().getBalance(toCur);
		*/
		
		/*
		 * MarketOrder order = new MarketOrder();
		order.setType(orderType);
		order.setTradableAmount(qty);
		order.setTradableIdentifier("BTC");
		order.setTransactionCurrency(localCurrency.getCurrencyCode());
		 */
		
		PollingTradeService tradeService = Application.getInstance().getExchange().getPollingTradeService();
		
		BigMoney balance = AccountManager.getInstance().getBalance(fromCur);
		BigMoney qty = balance.multipliedBy(AccountManager.getInstance().getLastTick(fromCur).getAsk().getAmount());
		
		String marketOrderReturnValue = "";
		boolean success = true;
				
		MarketOrder buyOrder  = new MarketOrder(OrderType.BID,balance.getAmount(),"BTC",fromCur.toString());
		MarketOrder sellOrder = new MarketOrder(OrderType.ASK,qty.getAmount(),"BTC",toCur.toString());
				
		marketOrderReturnValue = tradeService.placeMarketOrder(buyOrder);
		log.info(marketOrderReturnValue);
		if(success) {
			log.info("Arbitrage traded "+qty.toString()+" ");
		}else {
			log.info("Arbitrage could not trade "+qty.toString());
		}
		
		marketOrderReturnValue = tradeService.placeMarketOrder(sellOrder);
		log.info(marketOrderReturnValue);			
		if(success) {
			log.info("Successfully traded with Arbitrage!");
		}else {
			log.info("Failed to complete the recommended trade via Arbitrage, perhaps your balances were too low.");
		}
	}

	
	public synchronized Pair<CurrencyUnit,Double> getHighestBid() throws WalletNotFoundException{
			
		double highFactor = 1;
		
		CurrencyUnit highCurrency = baseCurrency;
		ATPTicker lastTick = AccountManager.getInstance().getLastTick(baseCurrency);
		
		Double basePrice = lastTick.getLast().getAmount().doubleValue();
		
		for(CurrencyUnit currency : bidMap.keySet()) {
			BigMoney balance = AccountManager.getInstance().getBalance(currency);
			if(balance.isZero()) {
				continue;
			}
			
			Double testPrice = bidMap.get(currency);
			factor = basePrice/testPrice;
			
			if(factor > highFactor) {
				highFactor = factor;
				highCurrency = currency;
			}
		}
				
		return new Pair<CurrencyUnit,Double>(highCurrency,highFactor);
	}

	public synchronized Pair<CurrencyUnit, Double> getLowestAsk() throws WalletNotFoundException {
		
		double lowFactor = 1;
		
		CurrencyUnit lowCurrency = baseCurrency;
		ATPTicker lastTick = AccountManager.getInstance().getLastTick(baseCurrency);
		
		Double basePrice = lastTick.getLast().getAmount().doubleValue();
		
		for(CurrencyUnit currency : askMap.keySet()) {
			
			BigMoney balance = AccountManager.getInstance().getBalance(currency);
			if(balance.isZero()) {
				continue;
			}

			Double testPrice = askMap.get(currency);
			factor = basePrice / testPrice;
			
			if(factor < lowFactor) {
				lowFactor = factor;
				lowCurrency = currency;
			}
		}
		
		return new Pair<CurrencyUnit,Double>(lowCurrency,lowFactor);
	}

	public synchronized void addTick(ATPTicker tick) {
		CurrencyUnit currency = CurrencyUnit.getInstance(tick.getLast().getCurrencyUnit().getCurrencyCode());
		Double bidPrice = tick.getBid().getAmount().doubleValue();
		Double askPrice = tick.getAsk().getAmount().doubleValue();
		
		if(!currency.getCode().equals("BTC")) {
			synchronized(bidMap) {
				bidMap.put(currency, bidPrice);
			}
			synchronized(askMap) {
				askMap.put(currency, askPrice);
			}
		}
		
	}
	public void stop() {
		quit = true;
	}
}
