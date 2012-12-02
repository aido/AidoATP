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
	private BigMoney vwap;
	private Exchange exchange;
	private PollingTradeService tradeService;
	private ATPTicker lastTick;
	private TrendObserver observer;
	private ArrayList<ATPTicker> ticker;
	private BigMoney maxBTC;
	private BigMoney minBTC;
	private BigMoney maxLocal;
	private BigMoney minLocal;
	private Double maxWeight;
	private Integer algorithm;
	private CurrencyUnit localCurrency;
	private Logger log;
	private StreamingTickerManager tickerManager;
	
	public TrendTradingAgent(TrendObserver observer) {
		log = LoggerFactory.getLogger(TrendTradingAgent.class);
		this.observer = observer;
		exchange = Application.getInstance().getExchange();
		tradeService = exchange.getPollingTradeService();
		tickerManager = observer.getTickerManager();
		localCurrency = tickerManager.getCurrency();
		maxBTC = BigMoney.of(CurrencyUnit.of("BTC"),new BigDecimal(Application.getInstance().getConfig("MaxBTC")));
		maxLocal = BigMoney.of(localCurrency,new BigDecimal(Application.getInstance().getConfig("MaxLocal")));
		minBTC = BigMoney.of(CurrencyUnit.of("BTC"),new BigDecimal(Application.getInstance().getConfig("MinBTC")));
		minLocal = BigMoney.of(localCurrency,new BigDecimal(Application.getInstance().getConfig("MinLocal")));
		maxWeight = new Double(Application.getInstance().getConfig("MaxLoss"));
		algorithm = new Integer(Application.getInstance().getConfig("Algorithm"));		
	}

	public void run(){
		
		trendArrow = observer.getTrendArrow();
		bidArrow = observer.getBidArrow();
		askArrow = observer.getAskArrow();
		vwap = observer.getVwap();
		lastTick = observer.getLastTick();
		ticker = tickerManager.getMarketData();
		
		StringBuilder str = new StringBuilder();
		str.append("Ticker Size: ");
		str.append(ticker.size());
		str.append(" | ");
		str.append("Trend Arrow: ");
		str.append(trendArrow);
		str.append(" | ");
		str.append("Bid Arrow: ");
		str.append(bidArrow);
		str.append(" | ");
		str.append("Ask Arrow: ");
		str.append(askArrow);
		str.append(" | ");
		str.append("VWAP: ");
		str.append(vwap);
		str.append(" | ");
		str.append("Long SMA: ");
		str.append(observer.getSMA(ticker.size()).toString());
		str.append(" | ");
		str.append("Short SMA: ");
		str.append(observer.getSMA(Integer.valueOf(Application.getInstance().getConfig("shortSMATickSize"))).toString());
		log.info(str.toString());
		
		str.setLength(0);
		str.append("The ");
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
		
		try {
			if (Application.getInstance().getTrendMode()) {
				if(trendArrow > 0 && bidArrow > 0){
					//If market is trending up, we should look at selling
					evalAsk();
				}else if(trendArrow < 0 && askArrow < 0){
					//If market is trending down, we should look at buying
					evalBid();
				}else {
					log.info("Trend following trading agent has decided no "+localCurrency.getCode()+" action will be taken at this time.");
				}
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
					Thread.currentThread().sleep(Constants.ONEMINUTE);
				} catch (InterruptedException e2) {
					e2.printStackTrace();
				}
			}
		} catch (Exception e) {
				log.error("ERROR: Caught unexpected exception, shutting down trend following trading agent now!. Details are listed below.");
				e.printStackTrace();
			}
	}

	//Let's decide whether or not to sell & how much to sell 
	private void evalAsk(){
		//Look at current bid
		BigMoney currentBid = lastTick.getBid();
		
		//Is currentBid > averageCost?
		if(currentBid.isGreaterThan(vwap)) {
			
			//Check balance and see if we even have anything to sell
			
			try {
				
				Double weight;
				//Look at bid arrow and calculate weight
				if(algorithm == 1) {
					weight = ((bidArrow + trendArrow) / ticker.size());
				}else {
					weight = (bidArrow / ticker.size()) * (trendArrow / ticker.size());
				}
				
				log.info("Weight is "+weight);
				weight = Math.abs(weight);
				
				if(weight > maxWeight) {
					log.info("Weight is above maxWeight, limiting weight to "+maxWeight);
					weight = maxWeight;
				}
				
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
					
					if(balanceBTC.isZero()) {
						log.info("BTC balance is empty. No further selling is possible until the market corrects or funds are added to your account.");
						return;
					}
					
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
					
					log.info("Trend following trade agent is attempting to sell "+qtyToSell.withScale(8,RoundingMode.HALF_UP).toString()+" of "+balanceBTC.toString()+" available");
					if(qtyToSell.compareTo(maxBTC) > 0) {
						log.info(qtyToSell.withScale(8,RoundingMode.HALF_UP).toString() + " was more than the configured limit of "+maxBTC.toString());
						log.info("Reducing order size to "+maxBTC.toString());
						qtyToSell = maxBTC;
					}
					if(qtyToSell.compareTo(minBTC) < 0) {
						log.info(qtyToSell.withScale(8,RoundingMode.HALF_UP).toString() + " was less than the configured limit of "+minBTC.toString());
						log.info("Trend following trade agent has decided that there is not enough "+localCurrency.getCode()+" momentum to trade at this time.");
						return;
					}
					if (!ArbitrageEngine.getInstance().getDisableTrendTrade()) {
						marketOrder(qtyToSell.getAmount(),OrderType.ASK);
					} else {
						log.info("Trend following trades disabled by Arbitrage Engine.");
					}
				}else{
					log.info("Could not determine wallet balance at this time, order will not be processed.");
				}
			}catch(WalletNotFoundException ex) {
				log.error("ERROR: Could not find wallet for "+localCurrency.getCurrencyCode());
				System.exit(1);
			}
			
		}else{
			log.info("Current bid price of "+currentBid.toString()+" is below the VWAP of "+vwap.toString());
			log.info("Trend following trade agent has determined that "+localCurrency.getCurrencyCode()+" market conditions are not favourable for you to sell at this time.");
		}
	}
	
	//Decide whether or not to buy and how much to buy
	private void evalBid(){
		//Look at current ask
		BigMoney currentAsk = lastTick.getAsk();
		if(currentAsk.isLessThan(vwap)) {
			//Formula for bid is the same as for ASK with USD/BTC instead of BTC/USD
			Double weight;
			
			//Look at bid arrow and calculate weight
			if(algorithm == 1) {
				weight = (askArrow + trendArrow) / ticker.size();
			}else {
				weight = (askArrow / ticker.size()) * (trendArrow / ticker.size());
			}
			
			weight = Math.abs(weight);
			
			log.info("Weight is "+weight);
			BigDecimal bigWeight = new BigDecimal(weight);			
			if(weight > maxWeight) {
				log.info("Weight is above maxWeight, limiting weight to "+maxWeight);
				weight = maxWeight;
			}
			
			BigMoney balanceLocal;
			try {
				
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
						
					if(balanceLocal.isZero()) {
						log.info(localCurrency+" balance is empty until the market corrects itself or funds are added to your account.");
						return;
					}
					
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
					
					log.info("Attempting to buy "+qtyToBuy.withScale(8,RoundingMode.HALF_UP).toString());
					if(qtyToBuy.compareTo(maxLocal) > 0){
						log.info(qtyToBuy.withScale(8,RoundingMode.HALF_UP).toString() +" was more than the configured maximum of "+maxLocal.toString()+". Reducing order size to "+maxLocal.toString());
						qtyToBuy = maxLocal;
					}
					
					if(qtyToBuy.compareTo(minLocal) < 0){
						log.info(qtyToBuy.withScale(8,RoundingMode.HALF_UP).toString() + " was less than the configured minimum of "+minLocal.toString());
						log.info("There just isn't enough momentum to trade at this time.");
						return;
					}
					if (!ArbitrageEngine.getInstance().getDisableTrendTrade()) {
						marketOrder(qtyToBuy.getAmount(),OrderType.BID);
					} else {
						log.info("Trend following trades disabled by Arbitrage Engine.");
					}
				}
			} catch (WalletNotFoundException e) {
				log.error("ERROR: Could not find wallet for "+localCurrency.getCurrencyCode());
				System.exit(1);
			}	
		}else{
			log.info("Current ask price of "+currentAsk.toString()+" is above the VWAP of "+vwap.toString());
			log.info("The trading agent has determined that "+localCurrency.getCurrencyCode()+" market conditions are not favourable for you to buy at this time.");
		}
	}
	
	private void marketOrder(BigDecimal qty, OrderType orderType) {
		MarketOrder order = new MarketOrder(orderType,qty,"BTC",localCurrency.getCurrencyCode());
		boolean success = true;
		
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
			log.info("Successfully"+action+qty.toPlainString()+" at current market price.");
			PLModel localProfit = AccountManager.getInstance().getPLFor(localCurrency);
			PLModel btcProfit = AccountManager.getInstance().getPLFor(CurrencyUnit.of("BTC"));
			
			log.info("Current P/L: "+btcProfit.getAmount()+" | "+btcProfit.getPercent()+"%");
			//log.info("Current P/L: "+localProfit.getAmount()+" | "+localProfit.getPercent()+"%" );
			
			Double overall;
			Double btc = btcProfit.getAmount().getAmount().doubleValue();
			Double local = localProfit.getAmount().getAmount().doubleValue();
			Double btcNormalized = btc * lastTick.getLast().getAmount().doubleValue();
			overall = local + btcNormalized;
			log.info("Overall P/L: "+overall+" "+localCurrency.getCurrencyCode());
			log.info(AccountManager.getInstance().getAccountInfo().toString());			
		}else{
			log.error("ERROR: Failed to"+failAction+qty.toPlainString()+" at current market price. Please investigate");
		}
	}
}
