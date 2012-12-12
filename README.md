Isis ATP
========

Isis Advanced Trading Platform courtesy of the Open Payment Alliance.

forked from openpay/OpenPay

What is Isis ATP?
================

Isis ATP is an automated trading platform primarily used for trading bitcoins (BTC) on various bitcoin exchanges.

How does it work?
=================

On startup Isis ATP goes into a learning mode for a configurable amount of time where it collects market data to be used by the trading algorithms. After the learning period is over the trading algorithms make a decision on whether it is favourable to trade that time. The trading algorithms currently implemented are:

Arbitrage algorithm
-------------------

Quite frequently there is a difference in the price of BTC in different currency pairs.

For instance, at time of writing, USD/BTC rate is $11.80 whereas the EUR/BTC rate is €9.14

At the same time the USD/EUR rate is 0.77 and the EUR/USD rate is 1.29

You could buy BTC in USD, then turn around and sell that same BTC back into EUR and make a profit of 0.52 EUR.  Next you buy BTC with all the 0.52EUR profit you just made and sell that BTC back to USD for a profit of 0.67USD

	1 BTC = 11.80 USD
	1 BTC = 9.14 EUR

	1 USD = 0.76 EUR
	1 EUR = 1.29 USD

The arbitrage engine uses the current trading algorithm to find the highest profit place to sell when it detects that the market conditions are right to sell.

Buys take place as normal, but only do so on the pair with the lowest cost real cost (BTCAsk * normalizing factor (pair1/pair2))

Advance/Decline Spread algorithm
--------------------------------

A simple Advance/Decline Spread algorithm is used to interpret the breadth of the market. This oscillator is extremely fast.

Exponential Moving Average based trend following algorithm
----------------------------------------------------------

The EMA algorithm reacts to trends very quickly but not as quick as the Advance/Decline Spread algorithm. It lags somewhat behind the Advance/Decline Spread algorithm. The EMA buy and sell decision is based on the crossover between two EMAs, one long and one short. The length of these moving averages is configurable.

Simple Moving Average based trend following algorithm
-----------------------------------------------------

The SMA algorithm reaction to trends is slower than both the EMA algorithm and the Advance/Decline Spread algorithm. Similar to the EMA, the SMA buy and sell deciscion is based on the crossover between two SMAs, one long and one short. The length of these moving averages is configurable.


Volume articipation algorithm (VWAP Cross)
------------------------------------------

The trend observer functionality constantly monitors the market for trends. A combination of the Advance/Decline Spread, SMA and EMA algorithms decide what way the market is trending.

	Market Trending Down = Look at buying
	Market Trending Up = Look at selling

Once it is decided the trend is up (ask) or down (bid) it then compares the last transaction to the VWAP.

The ratio of last price versus VWAP is used as a waterline to make the final determination that we will take an action.

	If trend = down & last < VWAP then buy
	If trend = up & last > VWAP then sell

Deciding when to buy or sell
============================
Any of the above algorithms may be disbled and not used in the buy / sell decision. Based on the reaction speed of each algorithm the following logic is used to make a buy or sell decision:

	Look to Sell if :
	
	Advance/Decline spread is trending up and EMA & SMA are disabled
			or
	Advance/Decline spread is trending up and EMA is trending down and SMA is disabled
			or
	Advance/Decline spread is trending up and EMA is trending down and SMA is trending down
			or
	Advance/Decline spread is trending up and EMA is disabled and SMA is trending down
			or
	Advance/Decline spread is disabled and EMA is trending up and SMA is trending down
			or
	Advance/Decline spread is disabled and EMA is trending up and SMA is disabled
			or
	Advance/Decline spread is disabled and EMA is disabled SMA is trending up

	Look to Buy if :
	
	Advance/Decline spread is trending down and EMA & SMA are disabled
			or
	Advance/Decline spread is trending down and EMA is trending up and SMA is disabled
			or
	Advance/Decline spread is trending down and EMA is trending up and SMA is trending up
			or
	Advance/Decline spread is trending down and EMA is disabled and SMA is trending up
			or
	Advance/Decline spread is disabled and EMA is trending down and SMA is trending up
			or
	Advance/Decline spread is disabled and EMA is trending down and SMA is disabled
			or
	Advance/Decline spread is disabled and EMA is disabled SMA is trending down
	
THE WRONG COMBINATION AND CONFIGURATION OF ANY OF THE ABOVE ALGORITHMS MAY LEAD TO SUBSTANTIAL LOSSES!!!

To protect against complete financial ruin, a VWAP cross algorithm is used to make the final call on a buy or sell decision. The VWAP cross algorithm may be disabled from the configuration file.

Once a final buy or sell decision has been made a stop loss value and risk calculation is used to determine the trade amount.

How much currency to use in a trade
===================================
Once we have decided to buy or sell, then we look at the ask, bid and trend arrows calculated from the Advance/Decline Spread Algorithm. This gives us an indication of how much momentum is in the current trend and is used to calculate a weight.

	current balance (BTC or local currency depending on Bid or Ask) * weight = how much we will be trading with.
	
Risk
====

The user has a choice of high risk or conservative trading.

The Trend Observer calculates the Advance/Decline spread for ask, bid and last prices to give a askArrow, bidArrow and trendArrow values. These values are then used to calculate a weight.

The weight is calculated one of two ways based on the choice of high risk or conservative and compared to stop loss value. If the calculated weight is above the stop loss value, weight is reduced to the configured stop loss value.

Here's how the weight is calculated for bid/buy:

	if(algorithm == 1) {
		weight = (askArrow + trendArrow) / ticker.size();
	}else {
		weight = (askArrow / ticker.size()) * (trendArrow / ticker.size());
	}
			
Algorithm 1 is high risk, algorithm 2 is conservative risk

Exchanges
=========

Isis ATP currently trades on the MtGOX exchange. More exchanges are planned.

Usage
=====

Isis ATP may be launched using the following command line:

	java -jar aido.jar

When run for the first time a configuration needs to be created. This can be done by adding the --clear-config switch and the user will then be interviewed:

	java -jar aido.jar --clear-config=true

The following switches are also available from the command line:

	--simulation-mode=true/false	Enable or disable a test/simulation mode where live trades will not be performed
	--use-arbitrage=true/false		Enable or disable the arbitrage trading engine
	--use-trend=true/false			Enable or disable the trend-following trading agent

The configuration file is stored in the ~/.java/.userPrefs/org/open/payment/alliance/isis/atp/prefs.xml file on Linux/UNIX systems or the HKEY_CURRENT_USER\Software\JavaSoft\Prefs\org\open\payment\alliance\isis\atp registry on Microsoft Windows systems.

When interviewed the user will be asked for the following information:

	MtGOX API key
	MtGOX secret key
	ISO Code for Prefered Currency (i.e. USD, GBP, JPY, EUR etc)
	Maximum number of bitcoins to trade in a single order
	Minimum number of bitcoins to trade in a single order
	Maximum amount of local currency to trade in a single order
	Minimum amount of local currency to trade in a single order
	Overall maximum loss tolerance (eg 25% = 0.25)
	Enable Arbitrage trading engine
	Minimum Profit to seek for Arbitrage (eg 10% = 0.10)
	Enable Trend-following trading engine
	Minimum ticker size for trend following trade decisions
	Maximum ticker age for trend following trade decisions (in minutes)
	Use Advance/Decline Spread algorithm
	Use Simple Moving Average algorithm (SMA)
	Use Exponential Moving Average algorithm (EMA)
	Number of ticks used to calculate short Moving Average
	Use VWAP Cross algorithm
	Which risk algorithm would you like to use? (High Risk or Conservative)
	Trading fee (eg 0.6% = 0.006)
		
If used, the --use-arbitrage and --use-trend command line switches will over-ride their respective configuration file values.
Use of --simulation-mode=false command line switch implies the user agrees with the license terms.

Practical example of Trend-following logic
==========================================

If we have 100 ticks in our ticker.
If of those ticks 60 were up and 40 were down the trendarrow will be +20, the market is now trending up.
During that same time there were 75 instances where the bidArrow went up and 25 where it went down, the bidArrow is now+50
The last trade was 10.25, the VWAP is 10.20.
Now is a good time to sell because there is enormous pressure in the market to buy.

So how much do we sell?
Let's assume we have 100 BTC.
trendArrow / ticker size = 20/100 = 0.2
bidArrow / ticker size = 0.5

Using the conservative risk weight calculation:

(0.2 * 0.5) =  10% of our 100 BTC balance.
100BTC * 0.1 = 10BTC

So now we sell 10 BTC at market price for 102.5 local currency.

Now an hour later the market has taken a dip.
We have 100 ticks in our ticker.
60 were down, 40 were up the trendArrow is now -20
There were 75 instances where the askArrow went down and 25 where it went up.  The askArrow is now -50
The last trade was 10.20, the current VWAP is 10.25

Our local currency balance is 102.5
(0.2 * 0.5) = 10% of our 102.5 local currency balance
102.5 * 0.1 = 10.25
We now buy 1.025 BTC for 10.25, this gives us a profit of 0.025 BTC

Now imagine that each of these trends continue on average for about 30 minutes before reversing and during that time we place 10 trades essentially identical (since these conditions are really only present for about 10 minutes out of each 30 minute trend), this is a profit of 0.25 BTC during that 30 minute period or 0.5 BTC per hour.
24 * 0.5 BTC = 12 BTC or 12% profit in a day.

		
Further reading
===============

A detailed discussion about this project may be found at: https://bitcointalk.org/index.php?topic=109831.0