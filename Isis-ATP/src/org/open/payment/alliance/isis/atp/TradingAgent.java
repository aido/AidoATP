/**
 * 
 */
package org.open.payment.alliance.isis.atp;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.logging.Logger;

import org.joda.money.BigMoney;
import org.joda.money.CurrencyUnit;

import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.dto.Order.OrderType;
import com.xeiam.xchange.dto.marketdata.Ticker;
import com.xeiam.xchange.dto.trade.AccountInfo;
import com.xeiam.xchange.dto.trade.MarketOrder;
import com.xeiam.xchange.service.trade.polling.PollingTradeService;

/**
 * @author Auberon
 * This is the agent that makes trades on behalf of the user.
 * 
 */
public class TradingAgent implements Runnable {

	private int trendArrow;
	private int bidArrow;
	private int askArrow;
	private BigMoney vwap;
	private Exchange exchange;
	private PollingTradeService tradeService;
	private ATPTicker lastTick;
	private TrendObserver observer;
	private ArrayList<ATPTicker> ticker;
	private BigDecimal maxBTC;
	private BigDecimal minBTC;
	private BigDecimal maxLocal;
	private BigDecimal minLocal;
	private BigDecimal maxWeight;
	private Integer    algorithm;
	private CurrencyUnit localCurrency;
	private Logger log;
		
	public TradingAgent(TrendObserver observer) {
		log = Logger.getLogger(TradingAgent.class.getSimpleName());
		this.observer = observer;
		exchange = IsisMtGoxExchange.getInstance();
		tradeService = exchange.getPollingTradeService();
				
		maxBTC = new BigDecimal(Application.getInstance().getConfig("MaxBTC"));
		maxLocal = new BigDecimal(Application.getInstance().getConfig("MaxLocal"));
		minBTC = new BigDecimal(Application.getInstance().getConfig("MinBTC"));
		minLocal = new BigDecimal(Application.getInstance().getConfig("MinLocal"));
		maxWeight = new BigDecimal(Application.getInstance().getConfig("MaxLoss"));
		localCurrency = CurrencyUnit.getInstance(Application.getInstance().getConfig("LocalCurrency"));
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
		ticker = TickerManager.getMarketData();
		
		if(trendArrow > 0){
			//If market is trending up, we should look at selling
			evalAsk();
		}else if(trendArrow < 0){
			//If market is trending down, we should look at buying
			evalBid();
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
				
				//Look at bid arrow and calculate weight
				BigDecimal weight = new BigDecimal((bidArrow / ticker.size()) * (trendArrow / ticker.size()));
								
				if(weight.compareTo(maxWeight) >0) {
					weight = maxWeight;
				}
				
				BigDecimal balance;
				
				
				balance = AccountManager.getInstance().getBalance(CurrencyUnit.of("BTC")).getAmount();
				
											
				if(balance != null) {
					
					if(balance.compareTo(BigDecimal.ZERO) == 0) {
						log.info("BTC Balance is empty.  No further selling is possible until the market corrects or funds are added to your account.");
						return;
					}
					
					BigDecimal qtyToSell;
					if(algorithm == 1) {
						qtyToSell = balance.multiply(weight);
					}else {
						if(balance.compareTo(maxBTC) >= 0) {
							qtyToSell = maxBTC.multiply(weight);
						}else {
							qtyToSell = balance.multiply(weight);
						}
					}
					
					log.info("Attempting to sell "+qtyToSell.toPlainString()+" of "+balance.toPlainString()+" BTC available");
					if(maxBTC != null) {
						if(qtyToSell.compareTo(maxBTC) > 0) {
							log.info(qtyToSell.toPlainString() + " was more than the configured limit of "+maxBTC.toPlainString()+"\nReducing order size to "+maxBTC);
							qtyToSell = maxBTC;
						}
						if(qtyToSell.compareTo(minBTC) < 0) {
							log.info(qtyToSell.toPlainString() + " was less than the configured limit of "+minBTC.toPlainString()+"\nIncreasing order size to "+minBTC.toPlainString());
							qtyToSell = minBTC;
						}
					}
				
					marketOrder(qtyToSell,OrderType.ASK);
				}else{
					log.info("Could not determine wallet balance at this time, order will not be processed.");
				}
			}catch(WalletNotFoundException ex) {
				log.severe("Could not find wallet for "+localCurrency.getCurrencyCode());
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
			BigDecimal weight = new BigDecimal((askArrow / ticker.size()) * (trendArrow / ticker.size()));
						
			if(weight.compareTo(maxWeight) >0) {
				weight = maxWeight;
			}
			
			BigDecimal balance;
			try {
				
					balance = AccountManager.getInstance().getBalance(localCurrency).getAmount();
				
			
				if(balance != null) {
					
					if(balance.compareTo(BigDecimal.ZERO) == 0) {
						log.info("Balance is empty.  No further selling is possible until the market corrects itself or funds are added to your account.");
						return;
					}
									
					BigDecimal qtyToBuy;
					
					if(algorithm == 1) {
						qtyToBuy = balance.multiply(weight);
					}else {
						if(balance.compareTo(maxLocal) >= 0) {
							qtyToBuy = maxLocal.multiply(weight);
						}else {
							qtyToBuy = balance.multiply(weight);
						}
					}
					
					log.info("Attempting to buy "+qtyToBuy.toPlainString()+" BTC");
					if(maxLocal != null){
						if(qtyToBuy.compareTo(maxLocal) > 0){
							log.info(qtyToBuy.toPlainString() + " was more than the configured maximum of "+maxLocal.toPlainString()+"\nReducing order size to "+maxLocal.toPlainString());
							qtyToBuy = maxLocal;
						}
						
						if(qtyToBuy.compareTo(minLocal) < 0){
							log.info(qtyToBuy.toPlainString() + " was less than the configured minimum of "+minLocal.toPlainString()+"\nIncreasing order size to "+minLocal.toPlainString());
							qtyToBuy = minLocal;
						}
					}
					marketOrder(qtyToBuy,OrderType.BID);
				}
			} catch (WalletNotFoundException e) {
				log.severe("Could not find wallet for "+localCurrency.getCurrencyCode());
				System.exit(1);
			}	
		}else{
			log.info("The trading agent has determined that market conditions are not appropriate for you to buy at this time.");
			log.info("Current ask price of "+currentAsk.toString()+" is below the VWAP of "+vwap.toString());
		}
	}
	
	private void marketOrder(BigDecimal qty, OrderType orderType) {
		MarketOrder order = new MarketOrder();
		order.setType(orderType);
		order.setTradableAmount(qty);
		order.setTradableIdentifier("BTC");
		order.setTransactionCurrency(localCurrency.getCurrencyCode());
		boolean success = true;
		
		if(!Application.getInstance().isSimMode()){
			success = tradeService.placeMarketOrder(order);
		}else{
			log.info("You are in simulation mode, the transaction below did NOT actually occur.");
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
			
			log.info("Current P/L: "+btcProfit.getAmount()+" %"+btcProfit.getPercent());
			log.info("Current P/L: "+localProfit.getAmount()+" %"+localProfit.getPercent() );
			
			log.info(AccountManager.getInstance().getAccountInfo().toString());
			
			
		}else{
			log.severe("Failed to"+failAction+qty.toPlainString()+" at current market price.\nPlease investigate");
		}
	}


}
