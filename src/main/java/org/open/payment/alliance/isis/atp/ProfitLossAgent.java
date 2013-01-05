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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.joda.money.BigMoney;
import org.joda.money.CurrencyUnit;

import com.xeiam.xchange.dto.trade.Wallet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProfitLossAgent implements Runnable {
	
	private static ProfitLossAgent instance = null;
	
	private HashMap<CurrencyUnit, ArrayList<BigMoney>> balances;
	private HashMap<CurrencyUnit, ArrayList<BigMoney>> rates;
	
	private static Logger log;
	
	public static ProfitLossAgent getInstance() {
		if(instance == null) {
			instance = new ProfitLossAgent();
		}
		return instance;
	}
	
	private ProfitLossAgent(){
			
			log = LoggerFactory.getLogger(ProfitLossAgent.class);
			balances = new HashMap<CurrencyUnit, ArrayList<BigMoney>>();
			rates = new HashMap<CurrencyUnit, ArrayList<BigMoney>>();
	}
	
	@Override
	public synchronized void run() {
	}
	
	public void updateBalances(List<Wallet> wallets) {
	
		for(Wallet wallet : wallets){
			CurrencyUnit currency = wallet.getBalance().getCurrencyUnit();
			
			//Do we have a new currency in our wallet?
			if(!balances.containsKey(currency)){
				//Make some space for it.
				balances.put(currency,new ArrayList<BigMoney>());
			}
			ArrayList<BigMoney> balance = balances.get(currency);
			// just store start and end balances
			if (balance.size() < 2) {
				balance.add(wallet.getBalance());
			} else {
				balance.set(1,wallet.getBalance());
			}
		}
	}
	
	public void updateRates(BigMoney rate) {
	
		CurrencyUnit currency = rate.getCurrencyUnit();
		
		//Do we have a new currency in our rates array?
		if(!rates.containsKey(currency)){
			//Make some space for it.
			rates.put(currency,new ArrayList<BigMoney>());
		}
		// just store first and last rate
		if (rates.get(currency).size() < 2) {
			rates.get(currency).add(rate);
		} else {
			rates.get(currency).set(1,rate);
		}
	}
	
	public void calcProfitLoss() {
	
		BigMoney normalisedBTCStartBal = BigMoney.zero(CurrencyUnit.of("BTC"));
		BigMoney normalisedBTCEndBal = BigMoney.zero(CurrencyUnit.of("BTC"));
		BigMoney startBal = BigMoney.zero(CurrencyUnit.of("BTC"));
		BigMoney endBal = BigMoney.zero(CurrencyUnit.of("BTC"));
		BigDecimal startRate = BigDecimal.ZERO;
		BigDecimal endRate = BigDecimal.ZERO;
		BigMoney profitBTC;
		BigDecimal profitPercent;
		NumberFormat percentFormat = NumberFormat.getPercentInstance();
		
		percentFormat.setMaximumFractionDigits(8);
		percentFormat.setRoundingMode(RoundingMode.HALF_EVEN);
		
		for(CurrencyUnit currency : balances.keySet()) {
			if (balances.get(currency).size() >= 2) {
				startBal = balances.get(currency).get(0);
				endBal = balances.get(currency).get(balances.get(currency).size() - 1);
				
				if (currency.equals(CurrencyUnit.of("BTC"))) {
					normalisedBTCStartBal = normalisedBTCStartBal.plus(startBal);
					normalisedBTCEndBal = normalisedBTCEndBal.plus(endBal);
				} else {
					if (startBal.isPositive() || endBal.isPositive()) {
						if (rates.containsKey(currency)) {
							if ((rates.get(currency).size() >= 2)) {
								startRate=rates.get(currency).get(0).getAmount();
								endRate=rates.get(currency).get(rates.get(currency).size() - 1).getAmount();
								
								normalisedBTCStartBal = normalisedBTCStartBal.plus(startBal.convertedTo(CurrencyUnit.of("BTC"),BigDecimal.ONE.divide(startRate,16,RoundingMode.HALF_EVEN)));
								normalisedBTCEndBal = normalisedBTCEndBal.plus(endBal.convertedTo(CurrencyUnit.of("BTC"),BigDecimal.ONE.divide(endRate,16,RoundingMode.HALF_EVEN)));
							} else {
								log.info("Not enough "+currency.getCode()+" ticker data collected yet to calculate profit/loss");
								return;
							}
						} else {
							log.info("No "+currency.getCode()+" ticker data collected yet, cannot calculate profit/loss");
							return;
						}
					}
				}
			} else {
				log.info("Not enough balance data collected yet to calculate profit/loss");
				return;
			}
		}
		profitBTC = normalisedBTCEndBal.minus(normalisedBTCStartBal);
		profitPercent = profitBTC.getAmount().divide(normalisedBTCStartBal.getAmount(),16,RoundingMode.HALF_EVEN);
		String profitToDisplay = percentFormat.format(profitPercent);
		log.info("Normalised BTC Start Balance: "+normalisedBTCStartBal.withScale(8,RoundingMode.HALF_EVEN).toString()+" Normalised BTC Current Balance: "+normalisedBTCEndBal.withScale(8,RoundingMode.HALF_EVEN).toString());
		log.info("BTC Profit/Loss: "+profitBTC.withScale(8,RoundingMode.HALF_EVEN).toString()+" Percentage Profit/Loss: "+profitToDisplay);
	}
}