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
import com.xeiam.xchange.service.polling.PollingTradeService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* Arbitrage engine class.
*
* @author Aido
*/

public class ArbitrageEngine implements Runnable {
	
	private static HashMap<String, ArbitrageEngine> instances  = new HashMap<String, ArbitrageEngine>();
	private CurrencyUnit baseCurrency;
	private double factor;
	private Logger log;
	private boolean quit;
	private boolean disableTrendTradeFlag;
	private HashMap<CurrencyUnit, ATPTicker> lastTickMap;
	private String exchangeName;

	public static synchronized ArbitrageEngine getInstance(String exchangeName) {
		if(!instances.containsKey(exchangeName)) {
			instances.put(exchangeName, new ArbitrageEngine(exchangeName));
		}
		return instances.get(exchangeName);
	}

	private ArbitrageEngine(String exchangeName) {
		this.exchangeName = exchangeName;
		log = LoggerFactory.getLogger(ArbitrageEngine.class);
		quit = false;
		disableTrendTradeFlag = false;
		lastTickMap = new HashMap<CurrencyUnit, ATPTicker>();
	}
	@Override
	public synchronized void run() {
	
		try {
			// V currencies
			int V = lastTickMap.size();
			CurrencyUnit[] currArray = lastTickMap.keySet().toArray(new CurrencyUnit[V]);
			
			// create complete network
			double rate;
			EdgeWeightedDigraph G = new EdgeWeightedDigraph(V);
			for(int v = 0; v < V; v++) {
				for(int w = 0; w < V; w++) {
					if (currArray[v].equals(currArray[w])) {
						rate = (double)1;
					} else {
						rate = lastTickMap.get(currArray[v]).getAsk().getAmount().divide(lastTickMap.get(currArray[w]).getBid().getAmount(),RoundingMode.HALF_EVEN).doubleValue();
					}
					DirectedEdge de = new DirectedEdge(v, w, -Math.log(rate));
					G.addEdge(de);
				}
			}

			// find negative cycle
			BellmanFordSP spt = new BellmanFordSP(G, 0);
			if (spt.hasNegativeCycle()) {
				for (DirectedEdge de : spt.negativeCycle()) {
					double fee = Double.parseDouble(Application.getInstance().getConfig("TradingFee"));
					double targetProfit = Double.parseDouble(Application.getInstance().getConfig("TargetProfit"));
					
					// Temporary value for testing purposes until I figure out how the hell to calculate profit
					double profit = 100;
					double profitAfterFee = profit - (fee * 2);

					NumberFormat percentFormat = NumberFormat.getPercentInstance();
					percentFormat.setMaximumFractionDigits(8);
					
					String profitToDisplay = percentFormat.format(profitAfterFee);
					
					log.debug(exchangeName+" Arbitrage profit after fee: "+profitAfterFee);
					
					if(profitAfterFee > targetProfit){
						log.info("Arbitrage Engine has detected an after fee profit opportunity of "+profitToDisplay
								+" on currency pair "+currArray[de.from()].toString()+"/"+currArray[de.to()].toString()+" on "+exchangeName);
						try {
							disableTrendTradeFlag = true;	//Lock out the other engine from trade execution while we arbitrage, any opportunities will still be there later.
							executeTrade(currArray[de.from()],currArray[de.to()]);
							disableTrendTradeFlag = false;
						} catch (WalletNotFoundException e) {
							e.printStackTrace();
						}
					}else {
						log.info("Arbitrage Engine cannot find a profitable arbitrage opportunity on "+exchangeName+" at this time.");
					}
				}
			}else {
				log.info("Arbitrage Engine cannot find an arbitrage opportunity on "+exchangeName+" at this time.");
			}
		} catch (com.xeiam.xchange.ExchangeException | si.mazi.rescu.HttpException e) {
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

	/**
	* Create 2 orders, a buy & a sell
	* @param fromCurr
	* @param toCurr
	* @throws WalletNotFoundException
	*/
	private synchronized void executeTrade(CurrencyUnit fromCurr, CurrencyUnit toCurr) throws WalletNotFoundException {
		
		PollingTradeService tradeService = ExchangeManager.getInstance(exchangeName).getExchange().getPollingTradeService();
		
		BigMoney lastTickAskFrom = lastTickMap.get(fromCurr).getAsk();
		BigMoney lastTickBidTo = lastTickMap.get(toCurr).getBid();
		BigDecimal oneDivFrom = BigDecimal.ONE.divide(lastTickAskFrom.getAmount(),16,RoundingMode.HALF_EVEN);
		BigDecimal oneDivTo = BigDecimal.ONE.divide(lastTickBidTo.getAmount(),16,RoundingMode.HALF_EVEN);
		
		log.debug("Last ticker Ask price was "+lastTickAskFrom.toString());		
		log.debug("BTC/"+fromCurr.toString()+" is "+oneDivFrom.toString());
		log.debug("Last ticker Bid price was "+lastTickBidTo.toString());
		log.debug("BTC/"+toCurr.toString()+" is "+oneDivTo.toString());
		
		BigMoney qtyFrom = AccountManager.getInstance(exchangeName).getBalance(fromCurr);
		BigMoney qtyFromBTC = qtyFrom.convertedTo(CurrencyUnit.of("BTC"),oneDivFrom);
		BigMoney qtyTo = qtyFromBTC.convertedTo(toCurr,lastTickBidTo.getAmount());
		BigMoney qtyToBTC = qtyTo.convertedTo(CurrencyUnit.of("BTC"),oneDivTo);

		if (!qtyFrom.isZero()){
			MarketOrder buyOrder  = new MarketOrder(OrderType.BID,qtyFromBTC.getAmount(),"BTC",fromCurr.toString());
			MarketOrder sellOrder = new MarketOrder(OrderType.ASK,qtyToBTC.getAmount(),"BTC",toCurr.toString());
			
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
