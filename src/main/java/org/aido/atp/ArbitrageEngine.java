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
import com.xeiam.xchange.service.trade.polling.PollingTradeService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* Arbitrage engine class.
*
* @author Aido
*/

public class ArbitrageEngine implements Runnable {
	
	private static HashMap<String, ArbitrageEngine> instances;
	private CurrencyUnit baseCurrency;
	private double factor;
	private Logger log;
	private boolean quit;
	private boolean disableTrendTradeFlag;
	private HashMap<CurrencyUnit, ATPTicker> lastTickMap;
	private static String exchangeName;
	
	private ArbitrageEngine() {
		log = LoggerFactory.getLogger(ArbitrageEngine.class);
		quit = false;
		disableTrendTradeFlag = false;
		lastTickMap = new HashMap<CurrencyUnit, ATPTicker>();
		baseCurrency = CurrencyUnit.getInstance(Application.getInstance().getConfig("LocalCurrency"));
	}

	public static synchronized ArbitrageEngine getInstance(String exchangeString) {
		exchangeName = exchangeString;
		if(instances.get(exchangeName) == null) {
			instances.put(exchangeName, new ArbitrageEngine());
		}
		return instances.get(exchangeName);
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
				
				double fee = Double.parseDouble(Application.getInstance().getConfig("TradingFee"));
				double targetProfit = Double.parseDouble(Application.getInstance().getConfig("TargetProfit"));
				
				//We buy from the lowestAsk & sell to the highestBid;
				double profit = highestBid.getAmount().subtract(lowestAsk.getAmount()).doubleValue();
				double profitAfterFee = profit - (fee * 2);

				NumberFormat percentFormat = NumberFormat.getPercentInstance();
				percentFormat.setMaximumFractionDigits(8);
				
				String profitToDisplay = percentFormat.format(profitAfterFee);
				
				log.debug(exchangeName+" Arbitrage profit after fee: "+profitAfterFee);
				
				if(profitAfterFee > targetProfit){
					log.info("Arbitrage Engine has detected an after fee profit opportunity of "+profitToDisplay
							+" on currency pair "+lowestAsk.getCurrencyUnit().toString()+"/"+highestBid.getCurrencyUnit().toString()+" on "+exchangeName);
					
					log.info(exchangeName+" Conversion Factors:- \tHighest Bid: "+highestBid.toString()+"\t Lowest Ask: "+lowestAsk.toString());
					
					try {
						disableTrendTradeFlag = true;	//Lock out the other engine from trade execution while we arbitrage, any opportunities will still be there later.
						executeTrade(lowestAsk,highestBid);
						disableTrendTradeFlag = false;
					} catch (WalletNotFoundException e) {
						e.printStackTrace();
					}
				}else {
					log.info("Arbitrage Engine cannot find a profitable opportunity on "+exchangeName+" at this time.");
				}
			} catch (com.xeiam.xchange.ExchangeException e) {
				Socket testSock = null;
				try {
					log.warn("WARNING: Testing connection to "+exchangeName+" exchange");
					testSock = new Socket(ExchangeManager.getInstance(exchangeName).getHost(),ExchangeManager.getInstance(exchangeName).getPort());
				}
				catch (java.io.IOException e1) {
					try {
						log.error("ERROR: Cannot connect to "+exchangeName+" exchange.");
						TimeUnit.MINUTES.sleep(1);
					} catch (InterruptedException e2) {
						e2.printStackTrace();
					}
				}
			} catch (Exception e) {
				log.error("ERROR: Caught unexpected exception, shutting down "+exchangeName+" arbitrage engine now!. Details are listed below.");
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
		
		PollingTradeService tradeService = ExchangeManager.getInstance(exchangeName).getExchange().getPollingTradeService();
		
		BigMoney lastTickAskFrom = lastTickMap.get(from.getCurrencyUnit()).getAsk();
		BigMoney lastTickBidTo = lastTickMap.get(to.getCurrencyUnit()).getBid();
		BigDecimal oneDivFrom = BigDecimal.ONE.divide(lastTickAskFrom.getAmount(),16,RoundingMode.HALF_EVEN);
		BigDecimal oneDivTo = BigDecimal.ONE.divide(lastTickBidTo.getAmount(),16,RoundingMode.HALF_EVEN);
		
		log.debug("Last ticker Ask price was "+lastTickAskFrom.toString());		
		log.debug("BTC/"+from.getCurrencyUnit().toString()+" is "+oneDivFrom.toString());
		log.debug("Last ticker Bid price was "+lastTickBidTo.toString());
		log.debug("BTC/"+to.getCurrencyUnit().toString()+" is "+oneDivTo.toString());
		
		BigMoney qtyFrom = AccountManager.getInstance(exchangeName).getBalance(from.getCurrencyUnit());
		BigMoney qtyFromBTC = qtyFrom.convertedTo(CurrencyUnit.of("BTC"),oneDivFrom);
		BigMoney qtyTo = qtyFromBTC.convertedTo(to.getCurrencyUnit(),lastTickBidTo.getAmount());
		BigMoney qtyToBTC = qtyTo.convertedTo(CurrencyUnit.of("BTC"),oneDivTo);

		if (!qtyFrom.isZero()){
			MarketOrder buyOrder  = new MarketOrder(OrderType.BID,qtyFromBTC.getAmount(),"BTC",from.getCurrencyUnit().toString());
			MarketOrder sellOrder = new MarketOrder(OrderType.ASK,qtyToBTC.getAmount(),"BTC",to.getCurrencyUnit().toString());
			
			log.debug(exchangeName+" Arbitrage buy order is buy "+qtyFromBTC.toString()+" for "+qtyFrom.toString());
			log.debug(exchangeName+" Arbitrage sell order is sell "+qtyToBTC.toString()+" for "+qtyTo.toString());
			
			String marketbuyOrderReturnValue;
			if(!Application.getInstance().getSimMode()){
				marketbuyOrderReturnValue = tradeService.placeMarketOrder(buyOrder);
				log.info(exchangeName+" Market Buy Order return value: " + marketbuyOrderReturnValue);
			}else{
				log.info("You were in simulation mode, the trade below did NOT actually occur.");
				marketbuyOrderReturnValue = "Simulation mode";
			}
			
			String marketsellOrderReturnValue;
			if (marketbuyOrderReturnValue != null && !marketbuyOrderReturnValue.isEmpty()){
				log.info("Arbitrage sold "+qtyFrom.withScale(8,RoundingMode.HALF_EVEN).toString() +" for "+ qtyFromBTC.withScale(8,RoundingMode.HALF_EVEN).toString()+" on "+exchangeName);				
				if(!Application.getInstance().getSimMode()){
					marketsellOrderReturnValue = tradeService.placeMarketOrder(sellOrder);
					log.info(exchangeName+" Market Sell Order return value: " + marketsellOrderReturnValue);
				}else{
					log.info("You were in simulation mode, the trade below did NOT actually occur.");
					marketsellOrderReturnValue = "Simulation mode";
				}				
				if (marketsellOrderReturnValue != null && !marketsellOrderReturnValue.isEmpty()){
					log.info("Arbitrage bought "+qtyTo.withScale(8,RoundingMode.HALF_EVEN).toString() +" for "+ qtyToBTC.withScale(8,RoundingMode.HALF_EVEN).toString()+" on "+exchangeName);
					log.info("Arbitrage successfully traded "+qtyFrom.toString()+" for "+qtyTo.toString()+" on "+exchangeName);
					log.info(AccountManager.getInstance(exchangeName).getAccountInfo().toString());	
					ProfitLossAgent.getInstance().calcProfitLoss();		
				} else {
					log.error("ERROR: Sell failed. Arbitrage could not trade "+qtyFrom.toString()+" with "+qtyTo.toString()+" on "+exchangeName);
				}
			} else {
				log.error("ERROR: Buy failed. Arbitrage could not trade "+qtyFrom.toString()+" with "+qtyTo.toString()+" on "+exchangeName);
			}
		} else {
			log.info("Arbitrage could not trade with a balance of "+qtyFrom.toString()+" on "+exchangeName);
		}
	}

	
	public synchronized BigMoney getHighestBid() throws WalletNotFoundException{
		
		BigMoney highFactor = BigMoney.of(baseCurrency,0.01);
		
		synchronized (lastTickMap) {
			BigMoney basePrice = lastTickMap.get(baseCurrency).getLast();
	
			for(CurrencyUnit currency : lastTickMap.keySet()) {
				
				BigMoney testPrice = lastTickMap.get(currency).getBid();
				
				BigMoney factor = basePrice.isSameCurrency(testPrice) ?
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
				
				BigMoney factor = basePrice.isSameCurrency(testPrice) ?
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