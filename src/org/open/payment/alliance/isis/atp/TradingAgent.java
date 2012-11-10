/**
* 
*/
package org.open.payment.alliance.isis.atp;

import java.text.NumberFormat;
import java.math.BigDecimal;
import java.util.ArrayList;

import org.joda.money.BigMoney;
import org.joda.money.CurrencyUnit;

import com.xeiam.xchange.Exchange;
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
public class TradingAgent implements Runnable {

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
	private Integer    algorithm;
	private CurrencyUnit localCurrency;
	private Logger log;
	private TickerManager tickerManager;
	
	public TradingAgent(TrendObserver observer) {
		log = LoggerFactory.getLogger(TradingAgent.class);
		this.observer = observer;
		exchange = Application.getInstance().getExchange();
		tradeService = exchange.getPollingTradeService();
		tickerManager = observer.getTickerManager();
		localCurrency = tickerManager.getCurrency();	
		BigMoney maxBTC = BigMoney.parse(CurrencyUnit.of("BTC")+" "+Application.getInstance().getConfig("MaxBTC"));
		BigMoney maxLocal = BigMoney.parse(localCurrency+" "+Application.getInstance().getConfig("MaxLocal"));
		BigMoney minBTC = BigMoney.parse(CurrencyUnit.of("BTC")+" "+Application.getInstance().getConfig("MinBTC"));
		BigMoney minLocal = BigMoney.parse(localCurrency+" "+Application.getInstance().getConfig("MinLocal"));
		maxWeight = new Double(Application.getInstance().getConfig("MaxLoss"));
				
		algorithm = new Integer(Application.getInstance().getConfig("Algorithm"));		
	}

	/* (non-Javadoc)
	* @see java.lang.Runnable#run()
	*/
	@Override
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
		
		if(trendArrow > 0 && bidArrow > 0){
			//If market is trending up, we should look at selling
			evalAsk();
		}else if(trendArrow < 0 && askArrow < 0){
			//If market is trending down, we should look at buying
			evalBid();
		}else {
			log.info("No action will be taken at this time.");
		}
	}

	//Let's decide whether or not to sell & how much to sell 
	private void evalAsk(){
		//Look at current bid
		BigMoney currentBid = lastTick.getBid();
		//vwap = vwap.minus(vwap); //used to force the condition for debugging purposes comment out when done.
		//Application.getInstance().setSimMode(true);
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
				
				if(balanceBTC != null) {
					
					if(balanceBTC.isZero()) {
						log.info("BTC Balance is empty.  No further selling is possible until the market corrects or funds are added to your account.");
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
					
					log.info("Attempting to sell "+qtyToSell.toString()+" of "+balanceBTC.toString()+" available");
					if(maxBTC != null) {
						if(qtyToSell.compareTo(maxBTC) > 0) {
							log.info(qtyToSell.toString() + " was more than the configured limit of "+maxBTC.toString()+"\nReducing order size to "+maxBTC);
							qtyToSell = maxBTC;
						}
						if(qtyToSell.compareTo(minBTC) < 0) {
							log.info(qtyToSell.toString() + " was less than the configured limit of "+minBTC.toString()+"\nThere just isn't enough momentum to trade at this time.");
							return;
						}
					}
					
					marketOrder(qtyToSell.getAmount(),OrderType.ASK);
				}else{
					log.info("Could not determine wallet balance at this time, order will not be processed.");
				}
			}catch(WalletNotFoundException ex) {
				log.error("Could not find wallet for "+localCurrency.getCurrencyCode());
				System.exit(1);
			}
			
		}else{
			log.info("The trading agent has determined that market conditions are not appropriate for you to sell at this time.");
			log.info("Current bid price of "+currentBid.toString()+" is below the VWAP of "+vwap.toString());

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
				
				if(balanceLocal != null) {
					
					if(balanceLocal.isZero()) {
						log.info("Balance is empty.  No further buying is possible until the market corrects itself or funds are added to your account.");
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
					
					log.info("Attempting to buy "+qtyToBuy.toString());
					if(maxLocal != null){
						if(qtyToBuy.compareTo(maxLocal) > 0){
							log.info(qtyToBuy.toString() + " was more than the configured maximum of "+maxLocal.toString()+". Reducing order size to "+maxLocal.toString());
							qtyToBuy = maxLocal;
						}
						
						if(qtyToBuy.compareTo(minLocal) < 0){
							log.info(qtyToBuy.toString() + " was less than the configured minimum of "+minLocal.toString()+". There just isn't enough momentum to trade at this time.");
							return;
						}
					}
					marketOrder(qtyToBuy.getAmount(),OrderType.BID);
				}
			} catch (WalletNotFoundException e) {
				log.error("Could not find wallet for "+localCurrency.getCurrencyCode());
				System.exit(1);
			}	
		}else{
			log.info("The trading agent has determined that market conditions are not appropriate for you to buy at this time.");
			log.info("Current ask price of "+currentAsk.toString()+" is above the VWAP of "+vwap.toString());
		}
	}
	
	private void marketOrder(BigDecimal qty, OrderType orderType) {
		MarketOrder order = new MarketOrder(orderType,qty,"BTC",localCurrency.getCurrencyCode());
		boolean success = true;
		
		if(!Application.getInstance().isSimMode()){
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
			log.error("Failed to"+failAction+qty.toPlainString()+" at current market price.\nPlease investigate");
		}
	}


}
