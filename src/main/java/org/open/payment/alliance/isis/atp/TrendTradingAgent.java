/**
 * Copyright (c) 2013 Aido
 * 
 * This file is part of Isis ATP.
 * 
 * Isis ATP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Isis ATP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Isis ATP.  If not, see <http://www.gnu.org/licenses/>.
 */
 package org.open.payment.alliance.isis.atp;

import java.text.NumberFormat;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.net.Socket;

import org.joda.money.BigMoney;
import org.joda.money.CurrencyUnit;

import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.ExchangeSpecification;
import com.xeiam.xchange.dto.Order.OrderType;
import com.xeiam.xchange.dto.trade.MarketOrder;
import com.xeiam.xchange.service.trade.polling.PollingTradeService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author Auberon
* This is the agent that makes trades on behalf of the user.
*
*/
public class TrendTradingAgent implements Runnable {

	private double trendArrow;
	private double bidArrow;
	private double askArrow;
	private double maxWeight;
	private Exchange exchange;
	private PollingTradeService tradeService;
	private ATPTicker lastTick;
	private TrendObserver observer;
	private BigMoney maxBTC;
	private BigMoney minBTC;
	private BigMoney maxLocal;
	private BigMoney minLocal;
	private int algorithm;
	private int tickerSize;
	private CurrencyUnit localCurrency;
	private Logger log;
	
	public TrendTradingAgent(TrendObserver observer) {
		log = LoggerFactory.getLogger(TrendTradingAgent.class);
		this.observer = observer;
		exchange = Application.getInstance().getExchange();
		tradeService = exchange.getPollingTradeService();
		localCurrency = observer.getCurrency();
		maxBTC = BigMoney.of(CurrencyUnit.of("BTC"),new BigDecimal(Application.getInstance().getConfig("MaxBTC")));
		maxLocal = BigMoney.of(localCurrency,new BigDecimal(Application.getInstance().getConfig("MaxLocal")));
		minBTC = BigMoney.of(CurrencyUnit.of("BTC"),new BigDecimal(Application.getInstance().getConfig("MinBTC")));
		minLocal = BigMoney.of(localCurrency,new BigDecimal(Application.getInstance().getConfig("MinLocal")));
		maxWeight = Double.parseDouble(Application.getInstance().getConfig("MaxLoss"));
		algorithm = Integer.parseInt(Application.getInstance().getConfig("Algorithm"));		
	}

	public void run(){
		
		boolean adsUp = false, adsDown = false;
		boolean emaUp = false, emaDown = false;
		boolean smaUp = false, smaDown = false;
		boolean vwapCrossUp = false, vwapCrossDown = false;
		boolean evalAsk = false, evalBid = false;
		boolean useADS = Application.getInstance().getConfig("UseADS").equals("1");
		boolean useSMA = Application.getInstance().getConfig("UseSMA").equals("1");
		boolean useEMA = Application.getInstance().getConfig("UseEMA").equals("1");
		boolean useVWAPCross = Application.getInstance().getConfig("UseVWAPCross").equals("1");
			
		NumberFormat numberFormat = NumberFormat.getNumberInstance();
		
		numberFormat.setMaximumFractionDigits(8);
		
		trendArrow = observer.getTrendArrow();
		bidArrow = observer.getBidArrow();
		askArrow = observer.getAskArrow();
		lastTick = observer.getLastTick();
		tickerSize = observer.getTickerSize();
					
		log.info(localCurrency.getCode()+" Ticker Size: "+tickerSize);
		StringBuilder str = new StringBuilder();
		
		// if Advance/Decline Spread algorithm is enabled, use it to decide trade action
		if (useADS){
			str.setLength(0);
			str.append(localCurrency.getCode());
			str.append(" Trend Arrow: ");
			str.append(trendArrow);
			str.append(" | ");
			str.append(localCurrency.getCode());
			str.append(" Bid Arrow: ");
			str.append(bidArrow);
			str.append(" | ");
			str.append(localCurrency.getCode());
			str.append(" Ask Arrow: ");
			str.append(askArrow);
			log.info(str.toString());
			
			str.setLength(0);
			str.append("Advance/Decline spread has determined that the ");
			str.append(localCurrency.getCode());
			str.append(" market is trending");
			if(trendArrow > 0) {
				//Market is going up, look at selling some BTC
				str.append(" up.");
			}else if(trendArrow < 0) {
				//Market is going down, look at buying some BTC
				str.append(" down.");
			}else {
				//Market is stagnant, hold position
				str.append(" flat.");
			}
			log.info(str.toString());
			
			if(trendArrow > 0 && bidArrow > 0){
				//If market is trending up, we should look at selling
				adsUp = true;
			}else if(trendArrow < 0 && askArrow < 0){
				//If market is trending down, we should look at buying
				adsDown = true;
			}
		}
		
		// if EMA algorithm is enabled, use it to decide trade action
		if (useEMA){
			BigMoney emaLong = observer.getLongEMA();
			BigMoney emaShort = observer.getShortEMA();
			
			str.setLength(0);
			str.append("Long EMA: ");
			str.append(localCurrency.getCode());
			str.append(" ");
			str.append(numberFormat.format(emaLong.getAmount()));
			str.append(" | ");
			str.append("Short EMA: ");
			str.append(localCurrency.getCode());
			str.append(" ");
			str.append(numberFormat.format(emaShort.getAmount()));
			log.info(str.toString());
			
			str.setLength(0);
			str.append("EMA has determined that the ");
			str.append(localCurrency.getCode());
			str.append(" market is trending");
			if(emaShort.isGreaterThan(emaLong)) {
				//Market is going up, look at selling some BTC
				emaUp = true;
				str.append(" up.");
			}else if(emaShort.isLessThan(emaLong)) {
				//Market is going down, look at buying some BTC
				emaDown = true;
				str.append(" down.");
			}else {
				//Market is stagnant, hold position
				str.append(" flat.");
			}
			log.info(str.toString());
		}
		
		// if SMA algorithm is enabled, use it to decide trade action
		if (useSMA){
			BigMoney smaLong = observer.getLongSMA();;
			BigMoney smaShort = observer.getShortSMA();;
			
			str.setLength(0);
			str.append("Long SMA: ");
			str.append(smaLong.toString());
			str.append(" | ");
			str.append("Short SMA: ");
			str.append(smaShort.toString());
			log.info(str.toString());

			str.setLength(0);
			str.append("SMA has determined that the ");
			str.append(localCurrency.getCode());
			str.append(" market is trending");
			if(smaShort.isGreaterThan(smaLong)) {
				//Market is going up, look at selling some BTC
				smaUp = true;
				str.append(" up.");
			}else if(smaShort.isLessThan(smaLong)) {
				//Market is going down, look at buying some BTC
				smaDown = true;
				str.append(" down.");
			}else {
				//Market is stagnant, hold position
				str.append(" flat.");
			}
			log.info(str.toString());
		}
		
		if (useVWAPCross){
			BigMoney vwap = observer.getVwap();
			//Look at current bid
			BigMoney currentBid = lastTick.getBid();
			//Look at current ask
			BigMoney currentAsk = lastTick.getAsk();
			
			str.setLength(0);
			str.append("VWAP: ");
			str.append(vwap);
			str.append(" | ");
			str.append("Current Bid: ");
			str.append(currentBid);
			str.append(" | ");
			str.append("Current Ask: ");
			str.append(currentAsk);
			log.info(str.toString());

			str.setLength(0);
			str.append("VWAP Cross algorithm has determined that the ");
			str.append(localCurrency.getCode());
			str.append(" market is trending");
			//Is currentBid > averageCost?
			if (currentBid.isGreaterThan(vwap)) {
				vwapCrossUp = true;
				log.debug("Current bid price of "+currentBid.toString()+" is above the VWAP of "+vwap.toString());				
				str.append(" up.");
			} else if (currentAsk.isLessThan(vwap)) {
				vwapCrossDown = true;
				log.debug("Current ask price of "+currentAsk.toString()+" is below the VWAP of "+vwap.toString());
				str.append(" down.");
			} else {
				str.append(" flat.");
			}	
			log.info(str.toString());
		}
		
		try {
			// Look to buy if :
			// AD spread is trending up and EMA & SMA are disabled
			//		or
			// AD spread is trending up and EMA is trending down and SMA is disabled
			// 		or
			// AD spread is trending up and EMA is trending down and SMA is trending down
			// 		or
			// AD spread is trending up and EMA is disabled and SMA is trending down
			// 		or
			// AD spread is disabled and EMA is trending up and SMA is trending down
			// 		or
			// AD spread is disabled and EMA is trending up and SMA is disabled
			// 		or
			// AD spread is disabled and EMA is disabled SMA is trending up
			
			evalBid = (adsUp && ((emaDown || !useEMA) && (smaDown || !useSMA))) || (!useADS && ((emaUp && (smaDown || !useSMA)) || (!useEMA && smaUp)));
					
			// Look to sell if :
			// AD spread is trending down and EMA & SMA are disabled
			//		or
			// AD spread is trending down and EMA is trending up and SMA is disabled
			//		or
			// AD spread is trending down and EMA is trending up and SMA is trending up
			// 		or
			// AD spread is trending down and EMA is disabled and SMA is trending up
			//		or
			// AD spread is disabled and EMA is trending down and SMA is trending up
			// 		or
			// AD spread is disabled and EMA is trending down and SMA is disabled
			// 		or
			// AD spread is disabled and EMA is disabled and SMA is trending down
			
			evalAsk = (adsDown && ((emaUp || !useEMA) && (smaUp || !useSMA))) || (!useADS && ((emaDown && (smaUp || !useSMA)) || (!useEMA && smaDown)));
			
			if (useVWAPCross) {
				evalAsk = evalAsk && vwapCrossUp;
				evalBid = evalBid && vwapCrossDown;
			}
			
			if (evalAsk) {
				evalAsk();
			}else if (evalBid) {
				evalBid();
			}else {
				log.info("Trend following trading agent has decided no "+localCurrency.getCode()+" action will be taken at this time.");
			}
		} catch (Exception e) {
				log.error("ERROR: Caught unexpected exception, shutting down trend following trading agent now!. Details are listed below.");
				e.printStackTrace();
			}
	}

	//Let's decide whether or not to sell & how much to sell
	private void evalAsk(){
		StringBuilder str = new StringBuilder();
		
		str.setLength(0);
		
		try {
			double weight;
			
			str.append("Used ");
			
			//Look at bid arrow and calculate weight
			if(algorithm == 1) {
				str.append("High");
				weight = (bidArrow + trendArrow) / tickerSize;
			}else {
				str.append("Conservative");
				weight = bidArrow / tickerSize * trendArrow / tickerSize;
			}
			
			weight = Math.abs(weight);
			
			str.append(" risk algorithm to calculate weight of ");
			str.append(weight);
			log.info(str.toString());
			
			if(weight > maxWeight) {
				log.info("Weight is above stop loss value, limiting weight to "+maxWeight);
				weight = maxWeight;
			}
			//Check balance and see if we even have anything to sell
			BigMoney balanceBTC = AccountManager.getInstance().getBalance(CurrencyUnit.of("BTC"));
			
			if (balanceBTC != null) {
				log.debug("BTC Balance: "+balanceBTC.toString());
			}else {
				log.error("ERROR: BTC Balance is null");
			}
			if (maxBTC != null) {
				log.debug("Max. BTC: "+maxBTC.toString());
			}else {
				log.error("ERROR: Max. BTC is null");
			}
			if (minBTC != null) {
				log.debug("Min. BTC: "+minBTC.toString());
			}else {
				log.error("ERROR: Min. BTC is null");
			}
							
			if(balanceBTC != null && maxBTC != null && minBTC != null) {				
				if(!balanceBTC.isZero()) {
					BigMoney qtyToSell;
					BigDecimal bigWeight = new BigDecimal(weight);
					if(algorithm == 1) {
						qtyToSell = balanceBTC.multipliedBy(bigWeight);
					}else {
						if(balanceBTC.compareTo(maxBTC) >= 0) {
							qtyToSell = maxBTC.multipliedBy(bigWeight);
						}else {
							qtyToSell = balanceBTC.multipliedBy(bigWeight);
						}
					}
					if(qtyToSell.isGreaterThan(maxBTC)) {
						log.info(qtyToSell.withScale(8,RoundingMode.HALF_EVEN).toString() + " was more than the configured limit of "+maxBTC.toString());
						log.info("Reducing order size to "+maxBTC.toString());
						qtyToSell = maxBTC;
					}
					if(qtyToSell.isLessThan(minBTC)) {
						log.info(qtyToSell.withScale(8,RoundingMode.HALF_EVEN).toString() + " was less than the configured limit of "+minBTC.toString());
						log.info("Trend following trade agent has decided that there is not enough "+localCurrency.getCode()+" momentum to trade at this time.");
					} else if (Application.getInstance().getArbMode()) {
						if (ArbitrageEngine.getInstance().getDisableTrendTrade()) {
							log.info("Trend following trades disabled by Arbitrage Engine.");
						}
					} else {
						log.info("Trend following trade agent is attempting to sell "+qtyToSell.withScale(8,RoundingMode.HALF_EVEN).toString()+" of "+balanceBTC.toString()+" available");
						marketOrder(qtyToSell.getAmount(),OrderType.ASK);
					}
				} else {
					log.info("BTC balance is empty. No further selling is possible until the market corrects or funds are added to your account.");
				}
			}else{
				log.info("Could not determine wallet balance at this time, order will not be processed.");
			}
		}catch(WalletNotFoundException e) {
			log.error("ERROR: Could not find wallet for "+localCurrency.getCurrencyCode());
			System.exit(1);
		}
		return;
	}
	
	//Decide whether or not to buy and how much to buy
	private void evalBid(){
		StringBuilder str = new StringBuilder();
		
		str.setLength(0);
		
		try {
			//Formula for bid is the same as for ASK with USD/BTC instead of BTC/USD
			double weight;
			
			str.append("Used ");
			
			//Look at bid arrow and calculate weight
			if(algorithm == 1) {
				str.append("High");
				weight = (askArrow + trendArrow) / tickerSize;
			}else {
				str.append("Conservative");
				weight = askArrow / tickerSize * trendArrow / tickerSize;
			}
			
			weight = Math.abs(weight);
			
			str.append(" risk algorithm to calculate weight of ");
			str.append(weight);
			log.info(str.toString());
			
			BigDecimal bigWeight = new BigDecimal(weight);			
			if(weight > maxWeight) {
				log.info("Weight is above stop loss value, limiting weight to "+maxWeight);
				weight = maxWeight;
			}
			
			BigMoney balanceLocal;
			balanceLocal = AccountManager.getInstance().getBalance(localCurrency);
			
			if (balanceLocal != null) {
				log.debug("Local Balance: "+balanceLocal.toString());
			}else {
				log.error("ERROR: Local Balance is null");
			}
			if (maxLocal != null) {
				log.debug("Max. Local: "+maxLocal.toString());
			}else {
				log.error("ERROR: Max. Local is null");
			}
			if (minLocal != null) {
				log.debug("Min. Local: "+minLocal.toString());
			}else {
				log.error("ERROR: Min. Local is null");
			}
			
			if(balanceLocal != null && maxLocal != null && minLocal != null) {
					
				if(!balanceLocal.isZero()) {
					BigMoney qtyToBuy;
					bigWeight = new BigDecimal(weight);
					if(algorithm == 1) {
						qtyToBuy = balanceLocal.multipliedBy(bigWeight);
					}else {
						if(balanceLocal.compareTo(maxLocal) >= 0) {
							qtyToBuy = maxLocal.multipliedBy(bigWeight);
						}else {
							qtyToBuy = balanceLocal.multipliedBy(bigWeight);
						}
					}
					
					if(qtyToBuy.isGreaterThan(maxLocal)){
						log.info(qtyToBuy.withScale(8,RoundingMode.HALF_EVEN).toString() +" was more than the configured maximum of "+maxLocal.toString()+". Reducing order size to "+maxLocal.toString());
						qtyToBuy = maxLocal;
					}
					
					if(qtyToBuy.isLessThan(minLocal)){
						log.info(qtyToBuy.withScale(8,RoundingMode.HALF_EVEN).toString() + " was less than the configured minimum of "+minLocal.toString());
						log.info("Trend following trade agent has decided that there is not enough "+localCurrency.getCode()+" momentum to trade at this time.");
					} else if (Application.getInstance().getArbMode()) {
						if (ArbitrageEngine.getInstance().getDisableTrendTrade()) {
							log.info("Trend following trades disabled by Arbitrage Engine.");
						}
					} else {
						// Convert local currency amount to BTC
						BigMoney qtyBTCToBuy = qtyToBuy.convertedTo(CurrencyUnit.of("BTC"),BigDecimal.ONE.divide(lastTick.getAsk().getAmount(),16,RoundingMode.HALF_EVEN));
						if(qtyBTCToBuy.isLessThan(minBTC)) {
							log.info(qtyBTCToBuy.withScale(8,RoundingMode.HALF_EVEN).toString() + " was less than the configured limit of "+minBTC.toString());
							log.info("Trend following trade agent has decided that there is not enough "+localCurrency.getCode()+" momentum to trade at this time.");
						} else {
							log.info("Trend following trade agent is attempting to buy "+qtyBTCToBuy.withScale(8,RoundingMode.HALF_EVEN).toString()+" at current "+localCurrency.getCurrencyCode()+" market price.");
							marketOrder(qtyBTCToBuy.getAmount(),OrderType.BID);
						}
					}
				} else {
					log.info(localCurrency+" balance is empty until the market corrects itself or funds are added to your account.");
				}				
			}
		} catch (WalletNotFoundException e) {
			log.error("ERROR: Could not find wallet for "+localCurrency.getCurrencyCode());
			System.exit(1);
		}
		return;
	}
	
	private void marketOrder(BigDecimal qty, OrderType orderType) {
		MarketOrder order = new MarketOrder(orderType,qty,"BTC",localCurrency.getCurrencyCode());
		boolean success = true;
		NumberFormat numberFormat = NumberFormat.getNumberInstance();
		
		numberFormat.setMaximumFractionDigits(8);
		
		if(!Application.getInstance().getSimMode()){
			String marketOrderReturnValue = tradeService.placeMarketOrder(order);
			log.info("Market Order return value: " + marketOrderReturnValue);
			success=(marketOrderReturnValue != null) ? true:false;
		}else{
			log.info("You were in simulation mode, the trade below did NOT actually occur.");
		}
		
		String action,failAction;
		if(orderType == OrderType.ASK) {
			action = " sold ";
			failAction = " sell ";
		}else {
			action = " bought ";
			failAction = " buy ";
		}
		
		if(success){
			log.info("Successfully"+action+numberFormat.format(qty)+" BTC at current "+localCurrency.getCurrencyCode()+" market price.");
			log.info(AccountManager.getInstance().getAccountInfo().toString());	
			ProfitLossAgent.getInstance().calcProfitLoss();		
		}else{
			log.error("ERROR: Failed to"+failAction+numberFormat.format(qty)+" BTC at current "+localCurrency.getCurrencyCode()+" market price. Please investigate");
		}
		return;
	}
}
