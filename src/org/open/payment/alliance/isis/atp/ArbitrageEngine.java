package org.open.payment.alliance.isis.atp;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;
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
	private CurrencyUnit baseCurrency;
	private double factor;
	private Logger log;
	private boolean quit;
	private boolean disableTrendTradeFlag;
	private HashMap<CurrencyUnit, ATPTicker> lastTickMap;
	
	private ArbitrageEngine() {
		log = LoggerFactory.getLogger(ArbitrageEngine.class);
		quit = false;
		disableTrendTradeFlag = false;
		lastTickMap = new HashMap<CurrencyUnit, ATPTicker>();
		baseCurrency = CurrencyUnit.getInstance(Application.getInstance().getConfig("LocalCurrency"));
	}

	public static synchronized ArbitrageEngine getInstance() {
		if(instance == null) {
			instance = new ArbitrageEngine();
		}
		return instance;
	}
	@Override
	public synchronized void run() {
	
		if (lastTickMap.get(baseCurrency) != null) {
			try {
				BigMoney highestBid = null;
				try {
					highestBid = getHighestBid();
				} catch (WalletNotFoundException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
				BigMoney lowestAsk = null;
				try {
					lowestAsk = getLowestAsk();
				} catch (WalletNotFoundException e1) {
					e1.printStackTrace();
				}
				
				Double fee = new Double(Application.getInstance().getConfig("TradingFee"));
				Double targetProfit = new Double(Application.getInstance().getConfig("TargetProfit"));
				
				//We buy from the lowestAsk & sell to the highestBid;
				Double profit = highestBid.getAmount().subtract(lowestAsk.getAmount()).doubleValue();
				Double profitAfterFee = profit - (fee * 2);

				NumberFormat percentFormat = NumberFormat.getPercentInstance();
				percentFormat.setMaximumFractionDigits(8);
				
				String profitToDisplay = percentFormat.format(profitAfterFee);
				
				log.debug("Arbitrage profit after fee: "+profitAfterFee);
				
				if(profitAfterFee > targetProfit){
					log.info("Arbitrage Engine has detected an after fee profit opportunity of "+profitToDisplay
							+" on currency pair "+lowestAsk.getCurrencyUnit().toString()+"/"+highestBid.getCurrencyUnit().toString());
					
					log.info("Conversion Factors:- \tHighest Bid: "+highestBid.toString()+"\t Lowest Ask: "+lowestAsk.toString());
					
					try {
						disableTrendTradeFlag = true;	//Lock out the other engine from trade execution while we arbitrage, any opportunities will still be there later.
						executeTrade(lowestAsk,highestBid);
						disableTrendTradeFlag = false;
					} catch (WalletNotFoundException e) {
						e.printStackTrace();
					}
				}else {
					log.info("Arbitrage Engine cannot find a profitable opportunity at this time.");
				}
			} catch (com.xeiam.xchange.PacingViolationException | com.xeiam.xchange.HttpException e) {
				ExchangeSpecification exchangeSpecification = Application.getInstance().getExchange().getDefaultExchangeSpecification();
				Socket testSock = null;
				try {
					log.warn("WARNING: Testing connection to exchange");
					testSock = new Socket(exchangeSpecification.getHost(),exchangeSpecification.getPort());
				}
				catch (java.io.IOException e1) {
					try {
						log.error("ERROR: Cannot connect to exchange.");
						TimeUnit.MINUTES.sleep(1);
					} catch (InterruptedException e2) {
						e2.printStackTrace();
					}
				}
			} catch (Exception e) {
				log.error("ERROR: Caught unexpected exception, shutting down arbitrage engine now!. Details are listed below.");
				e.printStackTrace();
			}
		}
	}

	
	/**
	* Create 2 orders, a buy & a sell
	* @param from
	* @param to
	* @throws WalletNotFoundException 
	*/
	private synchronized void executeTrade(BigMoney from, BigMoney to) throws WalletNotFoundException {
		
		PollingTradeService tradeService = Application.getInstance().getExchange().getPollingTradeService();
		
		BigMoney lastTickAskFrom = lastTickMap.get(from.getCurrencyUnit()).getAsk();
		BigMoney lastTickBidTo = lastTickMap.get(to.getCurrencyUnit()).getBid();
		BigDecimal oneDivFrom = BigDecimal.ONE.divide(lastTickAskFrom.getAmount(),16,RoundingMode.HALF_EVEN);
		BigDecimal oneDivTo = BigDecimal.ONE.divide(lastTickBidTo.getAmount(),16,RoundingMode.HALF_EVEN);
		
		log.debug("Last ticker Ask price was "+lastTickAskFrom.toString());		
		log.debug("BTC/"+from.getCurrencyUnit().toString()+" is "+oneDivFrom.toString());
		log.debug("Last ticker Bid price was "+lastTickBidTo.toString());
		log.debug("BTC/"+to.getCurrencyUnit().toString()+" is "+oneDivTo.toString());
		
		BigMoney qtyFrom = AccountManager.getInstance().getBalance(from.getCurrencyUnit());
		BigMoney qtyFromBTC = qtyFrom.convertedTo(CurrencyUnit.of("BTC"),oneDivFrom);
		BigMoney qtyTo = qtyFromBTC.convertedTo(to.getCurrencyUnit(),lastTickBidTo.getAmount());
		BigMoney qtyToBTC = qtyTo.convertedTo(CurrencyUnit.of("BTC"),oneDivTo);

		if (!qtyFrom.isZero()){
			MarketOrder buyOrder  = new MarketOrder(OrderType.BID,qtyFromBTC.getAmount(),"BTC",from.getCurrencyUnit().toString());
			MarketOrder sellOrder = new MarketOrder(OrderType.ASK,qtyToBTC.getAmount(),"BTC",to.getCurrencyUnit().toString());
			
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

	
	public synchronized BigMoney getHighestBid() throws WalletNotFoundException{
		
		BigMoney highFactor = BigMoney.of(baseCurrency,0.01);
		
		synchronized (lastTickMap) {
			BigMoney basePrice = lastTickMap.get(baseCurrency).getLast();
	
			for(CurrencyUnit currency : lastTickMap.keySet()) {
				
				BigMoney testPrice = lastTickMap.get(currency).getBid();
				
				BigMoney factor = basePrice.getCurrencyUnit() == testPrice.getCurrencyUnit() ?
							basePrice.dividedBy(testPrice.getAmount(),RoundingMode.HALF_EVEN) :
							basePrice.convertedTo(currency,BigDecimal.ONE.divide(testPrice.getAmount(),16,RoundingMode.HALF_EVEN));

				if(factor.getAmount().compareTo(highFactor.getAmount()) > 0 ) {
					highFactor = factor;
				}
			}
		}
		
		return highFactor;
	}

	public synchronized BigMoney getLowestAsk() throws WalletNotFoundException {
		
		BigMoney lowFactor = BigMoney.of(baseCurrency,100);
		
		synchronized (lastTickMap) {
			BigMoney basePrice = lastTickMap.get(baseCurrency).getLast();
			
			for(CurrencyUnit currency : lastTickMap.keySet()) {

				BigMoney testPrice = lastTickMap.get(currency).getAsk();
				
				BigMoney factor = basePrice.getCurrencyUnit() == testPrice.getCurrencyUnit() ?
							basePrice.dividedBy(testPrice.getAmount(),RoundingMode.HALF_EVEN) : 
							basePrice.convertedTo(currency,BigDecimal.ONE.divide(testPrice.getAmount(),16,RoundingMode.HALF_EVEN));							

				if(factor.getAmount().compareTo(lowFactor.getAmount()) < 0 ) {
					lowFactor = factor;
				}
			}
		}
		
		return lowFactor;
	}

	public void addTick(ATPTicker tick) {
		
		CurrencyUnit currency = CurrencyUnit.getInstance(tick.getLast().getCurrencyUnit().getCurrencyCode());
		
		if(!currency.getCode().equals("BTC")) {
			synchronized(lastTickMap) {
				lastTickMap.put(currency, tick);
			}
		}
		
	}
		
	public boolean getDisableTrendTrade() {
		return disableTrendTradeFlag;
	}
}
