package org.open.payment.alliance.isis.atp;

import java.util.HashMap;
import java.math.RoundingMode;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.net.Socket;

import org.joda.money.BigMoney;
import org.joda.money.CurrencyUnit;

import com.xeiam.xchange.dto.Order.OrderType;
import com.xeiam.xchange.dto.trade.MarketOrder;
import com.xeiam.xchange.ExchangeSpecification;
import com.xeiam.xchange.service.trade.polling.PollingTradeService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArbitrageEngine implements Runnable {
	
	private static ArbitrageEngine instance = null;
	private HashMap<CurrencyUnit, Double> askMap,bidMap;
	private CurrencyUnit baseCurrency;
	private double factor;
	private Logger log;
	
	
	private ArbitrageEngine() {
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
	public synchronized void run() {
		
		boolean wasTrendMode;
		
		try {				
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
			}
			
			Double fee = new Double(Application.getInstance().getConfig("TradingFee"));
			Double targetProfit = new Double(Application.getInstance().getConfig("TargetProfit"));
			
			//We buy from the lowestAsk & sell to the highestBid;
			double profit = highestBid.getSecond() - lowestAsk.getSecond();
			double profitAfterFee = profit - (fee *2);

			NumberFormat percentFormat = NumberFormat.getPercentInstance();
			percentFormat.setMaximumFractionDigits(8);
			
			String profitToDisplay = percentFormat.format(profitAfterFee);
			
			log.debug("Arbitrage profit after fee: "+profitAfterFee);
			
			if(profitAfterFee > targetProfit){
				log.info("Arbitrage Engine has detected an after fee profit opportunity of "+profitToDisplay
						+" on currency pair "+lowestAsk.getFirst()+"/"+highestBid.getFirst());
				
				log.info("Conversion Factors:- \tHighest Bid: "+highestBid.toString()+"\t Lowest Ask: "+lowestAsk.toString());
				
				try {
					wasTrendMode = Application.getInstance().getTrendMode();
					if (wasTrendMode) {
						log.debug("Disabling trend following trade agent to perform arbitrage trades");
						Application.getInstance().setTrendMode(false);	//Lock out the other engine from trade execution while we arbitrage, any opportunities will still be there later.
					}
					executeTrade(lowestAsk,highestBid);
					if (wasTrendMode) {
						log.debug("Re-enabling trend following trade agent after performing arbitrage trades");
						Application.getInstance().setTrendMode(wasTrendMode);
					}
				} catch (WalletNotFoundException e) {
					e.printStackTrace();
				}
				
			}else {
				log.info("Arbitrage Engine cannot find a profitable opportunity at this time.");
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
						Thread.currentThread().sleep(Constants.ONEMINUTE);
					} catch (InterruptedException e2) {
						e2.printStackTrace();
					}
				}
			}
		} catch (Exception e) {
			log.error("ERROR: Caught unexpected exception, shutting down arbitrage engine now!. Details are listed below.");
			e.printStackTrace();
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
			
			log.debug("Arbitrage buy order is buy "+qtyFromBTC.toString()+" for "+qtyFrom.toString());
			log.debug("Arbitrage sell order is sell "+qtyToBTC.toString()+" for "+qtyTo.toString());
			
			String marketbuyOrderReturnValue;
			if(!Application.getInstance().getSimMode()){
				marketbuyOrderReturnValue = tradeService.placeMarketOrder(buyOrder);
				log.info("Market Buy Order return value: " + marketbuyOrderReturnValue);
			}else{
				log.info("You were in simulation mode, the trade below did NOT actually occur.");
				marketbuyOrderReturnValue = "Simulation mode";
			}
			
			String marketsellOrderReturnValue;
			if (marketbuyOrderReturnValue != null && !marketbuyOrderReturnValue.isEmpty()){
				log.info("Arbitrage sold "+qtyFrom.toString() +" for "+ qtyFromBTC.rounded(8,RoundingMode.HALF_EVEN).toString());				
				if(!Application.getInstance().getSimMode()){
					marketsellOrderReturnValue = tradeService.placeMarketOrder(sellOrder);
					log.info("Market Sell Order return value: " + marketsellOrderReturnValue);
				}else{
					log.info("You were in simulation mode, the trade below did NOT actually occur.");
					marketsellOrderReturnValue = "Simulation mode";
				}				
				if (marketsellOrderReturnValue != null && !marketsellOrderReturnValue.isEmpty()){
					log.info("Arbitrage bought "+qtyTo.toString() +" for "+ qtyToBTC.rounded(8,RoundingMode.HALF_EVEN).toString());
					log.info("Arbitrage successfully traded "+qtyFrom.toString()+" for "+qtyTo.toString());
				} else {
					log.error("ERROR: Sell failed. Arbitrage could not trade "+qtyFrom.toString()+" with "+qtyTo.toString());
				}
			} else {
				log.error("ERROR: Buy failed. Arbitrage could not trade "+qtyFrom.toString()+" with "+qtyTo.toString());
			}
		} else {
			log.info("Arbitrage could not trade with a balance of "+qtyFrom.toString());
		}
	}

	
	public synchronized Pair<CurrencyUnit,Double> getHighestBid() throws WalletNotFoundException{
		
		double highFactor = 0.01;
		
		CurrencyUnit highCurrency = baseCurrency;
		
		ATPTicker lastTick = AccountManager.getInstance().getLastTick(baseCurrency);
		
		Double basePrice = lastTick.getLast().getAmount().doubleValue();
		
		synchronized (bidMap) {
		
			for(CurrencyUnit currency : bidMap.keySet()) {
				
				Double testPrice = bidMap.get(currency);
				factor = basePrice/testPrice;
				
				if(factor > highFactor) {
					highFactor = factor;
					highCurrency = currency;
				}
			}
		}
		
		return new Pair<CurrencyUnit,Double>(highCurrency,highFactor);
	}

	public synchronized Pair<CurrencyUnit, Double> getLowestAsk() throws WalletNotFoundException {
		
		double lowFactor = 100;
		
		CurrencyUnit lowCurrency = baseCurrency;
		ATPTicker lastTick = AccountManager.getInstance().getLastTick(baseCurrency);
		
		Double basePrice = lastTick.getLast().getAmount().doubleValue();
		
		synchronized (askMap) {
			for(CurrencyUnit currency : askMap.keySet()) {

				Double testPrice = askMap.get(currency);
				factor = basePrice / testPrice;
				
				if(factor < lowFactor) {
					lowFactor = factor;
					lowCurrency = currency;
				}
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
}
