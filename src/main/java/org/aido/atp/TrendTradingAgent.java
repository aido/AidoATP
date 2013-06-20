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

import java.text.NumberFormat;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.net.Socket;

import org.joda.money.BigMoney;
import org.joda.money.CurrencyUnit;

import org.codehaus.janino.ExpressionEvaluator;

import com.xeiam.xchange.dto.Order.OrderType;
import com.xeiam.xchange.dto.trade.MarketOrder;
import com.xeiam.xchange.service.polling.PollingTradeService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* Trend Trading Agent class.
*
* @author Aido
*/

public class TrendTradingAgent implements Runnable {

	private Integer trendArrow;
	private Integer bidArrow;
	private Integer askArrow;
	private Integer tickerSize;
	private Integer riskAlgorithm;
	private Double maxWeight;
	private HashMap<CurrencyUnit, Double> asksInARow;
	private HashMap<CurrencyUnit, Double> bidsInARow;
	private HashMap<String, Boolean> tradeIndicator;
	private PollingTradeService tradeService;
	private ATPTicker lastTick;
	private TrendObserver observer;
	private BigMoney maxBTC;
	private BigMoney minBTC;
	private BigMoney maxLocal;
	private BigMoney minLocal;
	private CurrencyUnit localCurrency;
	private Logger log;
	private boolean evalAsk;
	private boolean evalBid;
	private String exchangeName;

	public TrendTradingAgent(TrendObserver observer, String exchangeName) {
		log = LoggerFactory.getLogger(TrendTradingAgent.class);
		this.observer = observer;
		this.exchangeName = exchangeName;
		tradeService = ExchangeManager.getInstance(exchangeName).getExchange().getPollingTradeService();
		localCurrency = observer.getCurrency();
		maxBTC = BigMoney.of(CurrencyUnit.of("BTC"),new BigDecimal(Application.getInstance().getConfig("MaxBTC")));
		maxLocal = BigMoney.of(localCurrency,new BigDecimal(Application.getInstance().getConfig("MaxLocal")));
		minBTC = BigMoney.of(CurrencyUnit.of("BTC"),new BigDecimal(Application.getInstance().getConfig("MinBTC")));
		minLocal = BigMoney.of(localCurrency,new BigDecimal(Application.getInstance().getConfig("MinLocal")));
		maxWeight = Double.valueOf(Application.getInstance().getConfig("MaxLoss"));
		riskAlgorithm = Integer.valueOf(Application.getInstance().getConfig("RiskAlgorithm"));
		asksInARow = ExchangeManager.getInstance(exchangeName).getAsksInARow();
		bidsInARow = ExchangeManager.getInstance(exchangeName).getBidsInARow();
		if(!asksInARow.containsKey(localCurrency)){
			asksInARow.put(localCurrency,new Double(0));
		}
		if(!bidsInARow.containsKey(localCurrency)){
			bidsInARow.put(localCurrency,new Double(0));
		}
	}

	public void run(){

		String askLogic = Application.getInstance().getConfig("AskLogic");
		String bidLogic = Application.getInstance().getConfig("BidLogic");

		evalAsk = false;
		evalBid = false;

		tradeIndicator = new HashMap<String, Boolean>();
		tradeIndicator.put("ADS_Up",false);
		tradeIndicator.put("ADS_Down",false);
		tradeIndicator.put("EMA_Up",false);
		tradeIndicator.put("EMA_Down",false);
		tradeIndicator.put("SMA_Up",false);
		tradeIndicator.put("SMA_Down",false);
		tradeIndicator.put("VWAPCross_Up",false);
		tradeIndicator.put("VWAPCross_Down",false);
		tradeIndicator.put("MACD_Up",false);
		tradeIndicator.put("MACD_Down",false);
		tradeIndicator.put("MACD_Positive",false);
		tradeIndicator.put("MACD_Negative",false);

		NumberFormat numberFormat = NumberFormat.getNumberInstance();

		numberFormat.setMaximumFractionDigits(8);

		trendArrow = observer.getTrendArrow();
		bidArrow = observer.getBidArrow();
		askArrow = observer.getAskArrow();
		lastTick = observer.getLastTick();
		tickerSize = observer.getTickerSize();

		log.info(exchangeName + " {} Ticker Size: {}",localCurrency.getCode(), tickerSize);
		StringBuilder str = new StringBuilder();

		// if Advance/Decline Spread algorithm is enabled, use it to decide trade action
		if (askLogic.contains("ADS") || bidLogic.contains("ADS")){
			str.setLength(0);
			str.append(exchangeName + " " + localCurrency.getCode());
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
			log.debug(str.toString());

			str.setLength(0);
			str.append(exchangeName + " " + "Advance/Decline Spread algorithm has determined that the ");
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
				tradeIndicator.put("ADS_Up",true);
			}else if(trendArrow < 0 && askArrow < 0){
				//If market is trending down, we should look at buying
				tradeIndicator.put("ADS_Down",true);
			}
		}

		// if EMA algorithm is enabled, use it to decide trade action
		if (askLogic.contains("EMA") || bidLogic.contains("EMA")){
			BigMoney emaLong = observer.getLongEMA();
			BigMoney emaShort = observer.getShortEMA();

			str.setLength(0);
			str.append(exchangeName + ":- ");
			str.append("Long EMA: ");
			str.append(localCurrency.getCode());
			str.append(" ");
			str.append(numberFormat.format(emaLong.getAmount()));
			str.append(" | ");
			str.append("Short EMA: ");
			str.append(localCurrency.getCode());
			str.append(" ");
			str.append(numberFormat.format(emaShort.getAmount()));
			log.debug(str.toString());

			str.setLength(0);
			str.append(exchangeName + " ");
			str.append("EMA algorithm has determined that the ");
			str.append(localCurrency.getCode());
			str.append(" market is trending");
			if(emaShort.isGreaterThan(emaLong)) {
				//Market is going up, look at selling some BTC
				tradeIndicator.put("EMA_Up",true);
				str.append(" up.");
			}else if(emaShort.isLessThan(emaLong)) {
				//Market is going down, look at buying some BTC
				tradeIndicator.put("EMA_Down",true);
				str.append(" down.");
			}else {
				//Market is stagnant, hold position
				str.append(" flat.");
			}
			log.info(str.toString());
		}

		// if SMA algorithm is enabled, use it to decide trade action
		if (askLogic.contains("SMA") || bidLogic.contains("SMA")){
			BigMoney smaLong = observer.getLongSMA();;
			BigMoney smaShort = observer.getShortSMA();;

			str.setLength(0);
			str.append(exchangeName + ":- ");
			str.append("Long SMA: ");
			str.append(smaLong.toString());
			str.append(" | ");
			str.append("Short SMA: ");
			str.append(smaShort.toString());
			log.debug(str.toString());

			str.setLength(0);
			str.append(exchangeName + " ");
			str.append("SMA algorithm has determined that the ");
			str.append(localCurrency.getCode());
			str.append(" market is trending");
			if(smaShort.isGreaterThan(smaLong)) {
				//Market is going up, look at selling some BTC
				tradeIndicator.put("SMA_Up",true);
				str.append(" up.");
			}else if(smaShort.isLessThan(smaLong)) {
				//Market is going down, look at buying some BTC
				tradeIndicator.put("SMA_Down",true);
				str.append(" down.");
			}else {
				//Market is stagnant, hold position
				str.append(" flat.");
			}
			log.info(str.toString());
		}

		if (askLogic.contains("VWAPCross") || bidLogic.contains("VWAPCross")){
			BigMoney vwap = observer.getVwap();
			//Look at current bid
			BigMoney currentBid = lastTick.getBid();
			//Look at current ask
			BigMoney currentAsk = lastTick.getAsk();

			str.setLength(0);
			str.append(exchangeName + " VWAP: ");
			str.append(vwap);
			str.append(" | ");
			str.append("Current Bid: ");
			str.append(currentBid);
			str.append(" | ");
			str.append("Current Ask: ");
			str.append(currentAsk);
			log.debug(str.toString());

			str.setLength(0);
			str.append(exchangeName + " VWAP Cross algorithm has determined that the ");
			str.append(localCurrency.getCode());
			str.append(" market is trending");
			//Is currentBid > averageCost?
			if (currentBid.isGreaterThan(vwap)) {
				tradeIndicator.put("VWAPCross_Up",true);
				log.debug(exchangeName + "Current bid price of {} is above the VWAP of {}",currentBid.toString(),vwap.toString());
				str.append(" up.");
			} else if (currentAsk.isLessThan(vwap)) {
				tradeIndicator.put("VWAPCross_Down",true);
				log.debug(exchangeName + "Current ask price of {} is below the VWAP of {}",currentAsk.toString(),vwap.toString());
				str.append(" down.");
			} else {
				str.append(" flat.");
			}
			log.info(str.toString());
		}

		// if MACD algorithm is enabled, use it to decide trade action
		if (askLogic.contains("MACD") || bidLogic.contains("MACD")){
			BigMoney macdLong = observer.getLongMACD();
			BigMoney macdShort = observer.getShortMACD();
			BigMoney macdSigLine = observer.getSigLineMACD();
			BigMoney macdLine = macdShort.minus(macdLong);

			str.setLength(0);
			str.append(exchangeName + ": ");
			str.append("Long MACD: ");
			str.append(macdLong.withScale(8,RoundingMode.HALF_EVEN).toString());
			str.append(" | ");
			str.append("Short MACD: ");
			str.append(macdShort.withScale(8,RoundingMode.HALF_EVEN).toString());
			str.append(" | ");
			str.append("MACD Line: ");
			str.append(macdLine.withScale(8,RoundingMode.HALF_EVEN).toString());
			str.append(" | ");
			str.append("MACD Signal Line: ");
			str.append(macdSigLine.withScale(8,RoundingMode.HALF_EVEN).toString());
			log.debug(str.toString());

			str.setLength(0);
			str.append(exchangeName + " MACD has determined that the ");
			str.append(localCurrency.getCode());
			str.append(" market is trending");
			if(macdShort.isGreaterThan(macdLong)) {
				//Market is going up, look at selling some BTC
				tradeIndicator.put("MACD_Positive",true);
				str.append(" up.");
			}else if(macdShort.isLessThan(macdLong)) {
				//Market is going down, look at buying some BTC
				tradeIndicator.put("MACD_Negative",true);
				str.append(" down.");
			}else {
				//Market is stagnant, hold position
				str.append(" flat.");
			}
			log.info(str.toString());
			str.setLength(0);
			str.append(exchangeName + " MACD algorithm has determined that the ");
			str.append(localCurrency.getCode());
			str.append(" MACD Line is ");
			if(macdLine.isGreaterThan(macdSigLine)) {
				//Market is going up, look at selling some BTC
				tradeIndicator.put("MACD_Up",true);
				str.append("above");
			}else if(macdLine.isLessThan(macdLong)) {
				//Market is going down, look at buying some BTC
				tradeIndicator.put("MACD_Down",true);
				str.append("below");
			}else {
				//Market is stagnant, hold position
				str.append("equal to");
			}
			str.append(" the MACD Signal Line.");
			log.info(str.toString());
		}

		try {
			log.debug("Ask Logic: {}", askLogic);
			log.debug("Bid Logic: {}", bidLogic);

			if (evalLogic(askLogic)) {
				evalAsk();
			}else if (evalLogic(bidLogic)) {
				evalBid();
			}else {
				log.info("{} Trend following trading agent has decided no {} action will be taken at this time.",exchangeName,localCurrency.getCode());
			}
		} catch (Exception e) {
			log.error("ERROR: Caught unexpected exception, shutting down {} trend following trading agent now!. Details are listed below.",exchangeName);
			e.printStackTrace();
		}
	}

	//Decide whether or not to sell & how much to sell
	private void evalAsk(){
		StringBuilder str = new StringBuilder();
		Boolean disableTradeSet = false;

		str.setLength(0);

		try {
			Double weight;

			str.append("Used ");

			//Look at bid arrow and calculate weight
			if(riskAlgorithm.equals(1)) {
				str.append("Conservative");
				weight = (double)bidArrow / tickerSize * (double)trendArrow / tickerSize;
			} else if(riskAlgorithm.equals(2)) {
				str.append("High");
				weight = (double)(bidArrow + trendArrow) / tickerSize;
			} else if(riskAlgorithm.equals(3)) {
				str.append("Maximum");
				weight = (double)1;
			} else {
				// illegal value <1 or >3
				str.append("Conservative (Default)");
				weight = (double)bidArrow / tickerSize * (double)trendArrow / tickerSize;
			}

			weight = Math.abs(weight);

			str.append(" risk algorithm to calculate weight of ");
			str.append(weight.toString());
			log.info(str.toString());

			if(weight.compareTo(maxWeight) > 0) {
				log.info("Weight is above stop loss value, limiting weight to {}",maxWeight.toString());
				weight = maxWeight;
			}
			//Check balance and see if we even have anything to sell
			BigMoney balanceBTC = AccountManager.getInstance(exchangeName).getBalance(CurrencyUnit.of("BTC"));

			if (balanceBTC != null) {
				log.debug("{} BTC Balance: {}",exchangeName,balanceBTC.toString());
			}else {
				log.error("ERROR: {} BTC Balance is null.",exchangeName);
			}
			if (maxBTC != null) {
				log.debug("Max. BTC: {}",maxBTC.toString());
			}else {
				log.error("ERROR: Max. BTC is null");
			}
			if (minBTC != null) {
				log.debug("Min. BTC: {}",minBTC.toString());
			}else {
				log.error("ERROR: Min. BTC is null");
			}

			if(balanceBTC != null && maxBTC != null && minBTC != null) {
				if(!balanceBTC.isZero()) {
					BigMoney qtyToSell;
					BigDecimal bigWeight = new BigDecimal(weight / Math.pow(2, asksInARow.get(localCurrency)));
					if(riskAlgorithm.equals(1)) {
						qtyToSell = balanceBTC.multipliedBy(bigWeight);
					}else {
						if(balanceBTC.compareTo(maxBTC) >= 0) {
							qtyToSell = maxBTC.multipliedBy(bigWeight);
						}else {
							qtyToSell = balanceBTC.multipliedBy(bigWeight);
						}
					}
					if(qtyToSell.isGreaterThan(maxBTC)) {
						log.info("{} was more than the configured limit of {}",qtyToSell.withScale(8,RoundingMode.HALF_EVEN).toString(),maxBTC.toString());
						log.info("Reducing {} order size to {}",exchangeName,maxBTC.toString());
						qtyToSell = maxBTC;
					}
					if(qtyToSell.isLessThan(minBTC)) {
						log.info("{} was less than the configured limit of {}",qtyToSell.withScale(8,RoundingMode.HALF_EVEN).toString(),minBTC.toString());
						log.info("Trend following trade agent has decided that there is not enough {} momentum to trade at this time.",localCurrency.getCode());
					} else if (Application.getInstance().getArbMode()) {
						if (ExchangeManager.getInstance(exchangeName).getDisableTrade()) {
							log.info("{} Trend following trades disabled by another trade thread.",exchangeName);
						} else {
							ExchangeManager.getInstance(exchangeName).setDisableTrade(true);
							disableTradeSet = true;
							log.info(exchangeName + "Trend following trade agent is attempting to sell {} of {} available",qtyToSell.withScale(8,RoundingMode.HALF_EVEN).toString(),balanceBTC.toString());
							marketOrder(qtyToSell.getAmount(),OrderType.ASK);
						}
					}
				} else {
					log.info(exchangeName + "BTC balance is empty. No further selling is possible until the market corrects or funds are added to your account.");
				}
			}else{
				log.info("Could not determine {} wallet balance at this time, order will not be processed.",exchangeName);
			}
		}catch(WalletNotFoundException e) {
			log.error("ERROR: Could not find {} wallet for {}.",exchangeName,localCurrency.getCurrencyCode());
			System.exit(1);
		} finally {
			if (disableTradeSet) {
				ExchangeManager.getInstance(exchangeName).setDisableTrade(false);
			}
		} 
		return;
	}

	//Decide whether or not to buy and how much to buy
	private void evalBid(){
		StringBuilder str = new StringBuilder();
		Boolean disableTradeSet = false;

		str.setLength(0);

		try {
			//Formula for bid is the same as for ASK with USD/BTC instead of BTC/USD
			Double weight;

			str.append("Used ");

			//Look at ask arrow and calculate weight
			if(riskAlgorithm.equals(1)) {
				str.append("Conservative");
				weight = (double)askArrow / tickerSize * (double)trendArrow / tickerSize;
			} else if(riskAlgorithm.equals(2)) {
				str.append("High");
				weight = (double)(askArrow + trendArrow) / tickerSize;
			} else if(riskAlgorithm.equals(3)) {
				str.append("Maximum");
				weight = (double)1;
			} else {
				// illegal value <1 or >3
				str.append("Conservative (Default)");
				weight = (double)askArrow / tickerSize * (double)trendArrow / tickerSize;
			}

			weight = Math.abs(weight);

			str.append(" risk algorithm to calculate weight of ");
			str.append(weight.toString());
			log.info(str.toString());

			if(weight.compareTo(maxWeight) > 0) {
				log.info("Weight is above stop loss value, limiting weight to {}",maxWeight.toString());
				weight = maxWeight;
			}

			BigMoney balanceLocal;
			balanceLocal = AccountManager.getInstance(exchangeName).getBalance(localCurrency);

			if (balanceLocal != null) {
				log.debug("{} Local Balance: {}",exchangeName,balanceLocal.toString());
			}else {
				log.error("ERROR: {} Local Balance is null",exchangeName);
			}
			if (maxLocal != null) {
				log.debug("Max. Local: {}",maxLocal.toString());
			}else {
				log.error("ERROR: Max. Local is null");
			}
			if (minLocal != null) {
				log.debug("Min. Local: {}",minLocal.toString());
			}else {
				log.error("ERROR: Min. Local is null");
			}

			if(balanceLocal != null && maxLocal != null && minLocal != null) {

				if(!balanceLocal.isZero()) {
					BigMoney qtyToBuy;
					BigDecimal bigWeight = new BigDecimal(weight / Math.pow(2, bidsInARow.get(localCurrency)));
					if(riskAlgorithm.equals(1)) {
						qtyToBuy = balanceLocal.multipliedBy(bigWeight);
					}else {
						if(balanceLocal.compareTo(maxLocal) >= 0) {
							qtyToBuy = maxLocal.multipliedBy(bigWeight);
						}else {
							qtyToBuy = balanceLocal.multipliedBy(bigWeight);
						}
					}
					if(qtyToBuy.isGreaterThan(maxLocal)){
						log.info(qtyToBuy.withScale(8,RoundingMode.HALF_EVEN).toString() +" was more than the configured maximum of "+maxLocal.toString()+". Reducing " + exchangeName + "order size to "+maxLocal.toString());
						qtyToBuy = maxLocal;
					}

					if(qtyToBuy.isLessThan(minLocal)){
						log.info("{} was less than the configured minimum of {}",qtyToBuy.withScale(8,RoundingMode.HALF_EVEN).toString() ,minLocal.toString());
						log.info("{} Trend following trade agent has decided that there is not enough {} momentum to trade at this time.",exchangeName,localCurrency.getCode());
					} else if (Application.getInstance().getArbMode()) {
						if (ExchangeManager.getInstance(exchangeName).getDisableTrade()) {
							log.info("{} Trend following trades disabled by another trade thread.",exchangeName);
						} else {
							// Convert local currency amount to BTC
							BigMoney qtyBTCToBuy = qtyToBuy.convertedTo(CurrencyUnit.of("BTC"),BigDecimal.ONE.divide(lastTick.getAsk().getAmount(),16,RoundingMode.HALF_EVEN));
							if(qtyBTCToBuy.isLessThan(minBTC)) {
								log.info("{} was less than the configured limit of {}",qtyBTCToBuy.withScale(8,RoundingMode.HALF_EVEN).toString(),minBTC.toString());
								log.info("{} Trend following trade agent has decided that there is not enough {} momentum to trade at this time.",exchangeName,localCurrency.getCode());
							} else {
								ExchangeManager.getInstance(exchangeName).setDisableTrade(true);
								disableTradeSet = true;
								log.info(exchangeName + " Trend following trade agent is attempting to buy {} at current {} market price.",qtyBTCToBuy.withScale(8,RoundingMode.HALF_EVEN).toString(),localCurrency.getCurrencyCode());
								marketOrder(qtyBTCToBuy.getAmount(),OrderType.BID);
							}
						}
					}
				} else {
					log.info("{} balance is empty until the market corrects itself or funds are added to your account.",localCurrency);
				}
			}
		} catch (WalletNotFoundException e) {
			log.error("ERROR: Could not find wallet for {}",localCurrency.getCurrencyCode());
			System.exit(1);
		} finally {
			if (disableTradeSet) {
				ExchangeManager.getInstance(exchangeName).setDisableTrade(false);
			}
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
			log.info("{} Market Order return value: {}",exchangeName,marketOrderReturnValue);
			success=(marketOrderReturnValue != null) ? true:false;
		}else{
			log.info("You were in simulation mode, the {} trade below did NOT actually occur.",exchangeName);
		}

		String action,failAction;
		if(orderType.equals(OrderType.ASK)) {
			action = " sold ";
			failAction = " sell ";
			asksInARow.put(localCurrency,asksInARow.get(localCurrency) + 1);
			bidsInARow.put(localCurrency,new Double(0));
		}else {
			action = " bought ";
			failAction = " buy ";
			bidsInARow.put(localCurrency,bidsInARow.get(localCurrency) + 1);
			asksInARow.put(localCurrency,new Double(0));
		}
		ExchangeManager.getInstance(exchangeName).setAsksInARow(asksInARow);
		ExchangeManager.getInstance(exchangeName).setBidsInARow(bidsInARow);

		log.debug(exchangeName + " {} Asks in a row : {}",localCurrency.getCode(),asksInARow.get(localCurrency).toString());
		log.debug(exchangeName + " {} Bids in a row : {}",localCurrency.getCode(),bidsInARow.get(localCurrency).toString());

		if(success){
			log.info("Successfully"+action+numberFormat.format(qty)+" BTC at current " + exchangeName + " "+localCurrency.getCurrencyCode()+" market price.");
			log.info(AccountManager.getInstance(exchangeName).getAccountInfo().toString());
			ProfitLossAgent.getInstance().calcProfitLoss();
		}else{
			log.error("ERROR: Failed to"+failAction+numberFormat.format(qty)+" BTC at current " + exchangeName + " "+localCurrency.getCurrencyCode()+" market price. Please investigate");
		}
		return;
	}

	private boolean evalLogic(String tradeLogic) {

		boolean evalTrade = false;
		HashMap<String, Boolean> tradeIndic;

		// get unique collection of params
		Set<String> paramSet = new HashSet<String>(Arrays.asList(tradeLogic.split("\\W")));

		tradeIndic = new HashMap<String, Boolean>();

		for(String param : paramSet) {
			if ( param != null && param.length() != 0 ) {
				tradeIndic.put(param,tradeIndicator.get(param));
			}
		}

		String[] paramNames = tradeIndic.keySet().toArray(new String[0]);
		Class[] paramTypes = new Class[tradeIndic.size()];
		Arrays.fill(paramTypes,boolean.class);
		Object[] paramValues = tradeIndic.values().toArray();

		try {
			// Compile the expression once; relatively slow.
			ExpressionEvaluator eeTrade = new ExpressionEvaluator(
				tradeLogic,		// expression
				boolean.class,	// expressionType
				paramNames,		// parameterNames
				paramTypes		// parameterTypes
			);

			// Evaluate it with varying parameter values; very fast.
			evalTrade = (Boolean) eeTrade.evaluate(
				paramValues		// parameterValues
			);
		} catch (IllegalArgumentException e) {
			log.error("ERROR: Caught unexpected exception. Please check trend trading logic. Shutting down {} trend following trading agent now!. Details are listed below.",exchangeName);
			e.printStackTrace();
		} catch (org.codehaus.commons.compiler.CompileException | java.lang.reflect.InvocationTargetException e) {
			log.error("ERROR: Caught unexpected exception, shutting down {} trend following trading agent now!. Details are listed below.",exchangeName);
			e.printStackTrace();
		}

		return evalTrade;
	}
}
