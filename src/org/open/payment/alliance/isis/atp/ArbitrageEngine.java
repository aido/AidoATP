package org.open.payment.alliance.isis.atp;

import java.util.HashMap;
import java.math.RoundingMode;
import java.math.BigDecimal;
import java.text.NumberFormat;

import org.joda.money.BigMoney;
import org.joda.money.CurrencyUnit;

import com.xeiam.xchange.dto.Order.OrderType;
import com.xeiam.xchange.dto.trade.MarketOrder;
import com.xeiam.xchange.service.trade.polling.PollingTradeService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArbitrageEngine implements Runnable {
	
	private static ArbitrageEngine instance = null;
	private boolean quit;
	private HashMap<CurrencyUnit, Double> askMap,bidMap;
	private CurrencyUnit baseCurrency;
	private double factor;
	private Logger log;
	
	
	private ArbitrageEngine() {
		quit= false;
		log = LoggerFactory.getLogger(ArbitrageEngine.class);
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
		
		boolean wasSimMode;
		
		while(!quit) {
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

			NumberFormat percentFormat = NumberFormat.getPercentInstance();
			percentFormat.setMaximumFractionDigits(8);
			
			String profitToDisplay = percentFormat.format(profitAfterFee);	
			
			if(profitAfterFee > targetProfit){
				log.info("Arbitrage Engine has detected an after fee profit opportunity of "+profitToDisplay
				+" on currency pair "+lowestAsk.getFirst()+"/"+highestBid.getFirst());
				
				log.info("***Conversion Factors***");
				log.info("Highest Bid: "+highestBid.toString());
				log.info("Lowest Ask: "+lowestAsk.toString());
				
				try {
					wasSimMode = Application.getInstance().isSimMode();
					Application.getInstance().setSimMode(true); //Lock out the other engine from trade execution while we arbitrage, any opportunities will still be there later.
					executeTrade(lowestAsk,highestBid);
					Application.getInstance().setSimMode(wasSimMode);
				} catch (WalletNotFoundException e) {
					e.printStackTrace();
				}
				
			}else {
				log.info("Arbitrage Engine cannot find a profitable opportunity at this time.");
			}
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
		
		BigMoney lastTickAskFrom = AccountManager.getInstance().getLastTick(fromCur).getAsk();
		BigMoney lastTickBidTo = AccountManager.getInstance().getLastTick(toCur).getBid();
		BigDecimal oneDivFrom = BigDecimal.ONE.divide(lastTickAskFrom.getAmount(),16, RoundingMode.HALF_UP);
		BigDecimal oneDivTo = BigDecimal.ONE.divide(lastTickBidTo.getAmount(),16,RoundingMode.HALF_UP);
		
		log.debug("Last ticker Ask price was "+lastTickAskFrom.toString());		
		log.debug("BTC/"+fromCur.toString()+" is "+oneDivFrom.toString());
		log.debug("Last ticker Bid price was "+lastTickBidTo.toString());
		log.debug("BTC/"+toCur.toString()+" is "+oneDivTo.toString());
		
		BigMoney qtyFrom = AccountManager.getInstance().getBalance(fromCur);
		BigMoney qtyFromBTC = qtyFrom.convertedTo(CurrencyUnit.of("BTC"),oneDivFrom);
		BigMoney qtyTo = qtyFromBTC.convertedTo(toCur,lastTickBidTo.getAmount());
		BigMoney qtyToBTC = qtyTo.convertedTo(CurrencyUnit.of("BTC"),oneDivTo);

		if (!qtyFrom.isZero()){
			MarketOrder buyOrder  = new MarketOrder(OrderType.BID,qtyFromBTC.getAmount(),"BTC",fromCur.toString());
			MarketOrder sellOrder = new MarketOrder(OrderType.ASK,qtyToBTC.getAmount(),"BTC",toCur.toString());
			
			log.debug("Attemting to buy "+qtyTo.toString()+" with "+qtyFrom.toString());
			log.debug("Arbitrage buy order is buy "+qtyFromBTC.toString()+" for "+fromCur.toString());
			log.debug("Arbitrage sell order is sell "+qtyToBTC.toString()+" for "+toCur.toString());
			
			String marketbuyOrderReturnValue = tradeService.placeMarketOrder(buyOrder);
			log.info("Market Buy Order return value: " + marketbuyOrderReturnValue);
			if (marketbuyOrderReturnValue != null){
				log.info("Arbitrage sold "+qtyFrom.toString() +" for "+ qtyFromBTC.rounded(8,RoundingMode.HALF_EVEN).toString());
				String marketsellOrderReturnValue = tradeService.placeMarketOrder(sellOrder);
				log.info("Market Sell Order return value: " + marketsellOrderReturnValue);			
				if (marketbuyOrderReturnValue != null){
					log.info("Arbitrage bought "+qtyTo.toString() +" for "+ qtyToBTC.rounded(8,RoundingMode.HALF_EVEN).toString());
					log.info("Successfully traded "+qtyFrom.toString()+" for "+qtyTo.toString() +" with Arbitrage!");
				} else {
					log.error("Sell failed. Arbitrage could not trade "+qtyFrom.toString()+" with "+qtyTo.toString());
				}

			} else {
				log.error("Buy failed. Arbitrage could not trade "+qtyFrom.toString()+" with "+qtyTo.toString());
			}
		} else {
			log.info("Arbitrage could not trade a balance of "+qtyFrom.toString());
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

	public void addTick(ATPTicker tick) {
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
