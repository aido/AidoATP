Aido ATP
========

Aido Advanced Trading Platform 

Aido ATP is forked from IsisATP courtesy of the Open Payment Alliance.

What is Aido ATP?
================

Aido ATP is an automated trading platform primarily used for trading bitcoins (BTC) on various bitcoin exchanges.

How does it work?
=================

On startup Aido ATP goes into a learning mode for a configurable amount of time where it collects market data to be used by the trading algorithms. After the learning period is over every time a new tick is received the trading algorithms make a decision on whether it is favourable to trade at that time. The trading algorithms currently implemented are:

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


Volume Participation algorithm (VWAP Cross)
------------------------------------------

The trend observer functionality constantly monitors the market for trends. A combination of the Advance/Decline Spread, SMA and EMA algorithms decide what way the market is trending.

	Market Trending Down = Look at buying
	Market Trending Up = Look at selling

Once it is decided the trend is up (ask) or down (bid) it then compares the last transaction to the VWAP.

The ratio of last price versus VWAP is used as a waterline to make the final determination that we will take an action.

	If trend = down & last < VWAP then buy
	If trend = up & last > VWAP then sell
	
Moving Average Convergence-Divergence algorithm
-----------------------------------------------
The MACD line turns two trend-following indicators, exponential moving averages, into a momentum oscillator by subtracting the longer moving average from the shorter moving average. A MACD Signal Line is calulated from EMA of the MACD Line.


Deciding when to buy or sell
============================
Any of the above trending algorithms may be used to make a trade decision. The trading logic is dynamic and is contained in the configuration file.

Boolean logic is used with the following parameters for each trading algorithm:

Advance/Decline Spread algorithm:				ADS_Up and ADS_Down

Exponential Moving Average algorithm:			EMA_Up and EMA_Down

Simple Moving Average algorithm:				SMA_Up and SMA_Down

Volume Participation algorithm (VWAP Cross):	VWAPCross_Up and VWAPCross_Down

Moving Average Convergence-Divergence:			MACD_Positive and MACD_Negative	(MACD line above or below zero)

Moving Average Convergence-Divergence:			MACD_Up and MACD_Down (MACD line above or below MACD Signal line)

The following logical operators may be used:

	Operator	Result
	--------	------
		&		Logical AND

		|		Logical OR

		^		Logical XOR

		||		Short-circuit OR

		&&		Short-circuit AND

		!		Logical unary NOT
		
Expressions may be grouped using parentheses i.e. '(' and ')'.

For example, the following logic may be used in the configuration file to trigger a trade when the Advance/Decline Spread algorithm indicates that the market is trending down but the both the EMA and SMA algorithms indicate that the market is trending up.

	ADS_Down && (EMA_Up && SMA_Up)

THE WRONG CONFIGURATION OF ANY OF THE ABOVE ALGORITHMS MAY LEAD TO SUBSTANTIAL LOSSES!!!

Once a trade decision has been made a stop loss value and risk calculation is used to determine the trade amount.


How much currency to use in a trade
===================================
Once we have decided to buy or sell, then we look at the ask, bid and trend arrows calculated from the Advance/Decline Spread Algorithm. This gives us an indication of how much momentum is in the current trend and is used to calculate a weight.

	current balance (BTC or local currency depending on Bid or Ask) * weight = how much we will be trading with.
	
Risk
====

The user has a choice of conservative, high or maximun risk trading.

The Trend Observer calculates the Advance/Decline spread for ask, bid and last prices to give a askArrow, bidArrow and trendArrow values. These values are then used to calculate a weight.

The weight is calculated one of two ways based on the choice of high risk or conservative and compared to stop loss value. If the calculated weight is above the stop loss value, weight is reduced to the configured stop loss value.

Here's how the weight is calculated for Bid/Buy:

	if(algorithm == 1) {
		//Conservative
		weight = (askArrow / tickerSize) * (trendArrow / tickerSize);
	} else if(algorithm == 2) {
		//High
		weight = (askArrow + trendArrow) / tickerSize;
	} else if(algorithm == 3) {
		//Maximum
		weight = 1;
	} else {
		// illegal value <1 or >3
		// Conservative (Default)
		weight = (askArrow / tickerSize) * (trendArrow / tickerSize);
	}
	
Ask/Sell weight is calulated in a similar way.
			
Algorithm 1 is conservative risk, algorithm 2 is high risk and algorithm 3 is maximum risk.

Exchanges
=========

Aido ATP currently trades on the MtGox, BTC-e, Bitstamp and CampBX exchanges.

Usage
=====

Aido ATP may be launched using the following command line:

	java -jar aidoatp.jar

When run for the first time a configuration needs to be created. This can be done by adding the --clear-config switch and the user will then be interviewed:

	java -jar aidoatp.jar --clear-config=true

The following switches are also available from the command line:

	--simulation-mode=true/false	Enable or disable a test/simulation mode where live trades will not be performed
	--use-arbitrage=true/false		Enable or disable the arbitrage trading engine
	--use-trend=true/false			Enable or disable the trend-following trading agent

The configuration file is stored in the ~/.java/.userPrefs/org/aido/atp/prefs.xml file on Linux/UNIX systems or the HKEY_CURRENT_USER\Software\JavaSoft\Prefs\org\aido\atp registry on Microsoft Windows systems.

When interviewed the user will be asked for the following information:

	Use MtGOX exchange (y/n)
		MtGOX API key
		MtGOX secret key
	Use BTC-e exchange (y/n)
		BTC-e API key
		BTC-e secret key
	Use Bitstamp exchange (y/n)
		Bitstamp Username
		Bitstamp Password
	Use Bitcoin Central exchange (y/n)
		Bitcoin Central Username
		Bitcoin Central Password
	ISO Code for Prefered Currency (i.e. USD, GBP, JPY, EUR etc)
	Maximum number of bitcoins to trade in a single order
	Minimum number of bitcoins to trade in a single order
	Maximum amount of local currency to trade in a single order
	Minimum amount of local currency to trade in a single order
	Overall maximum loss tolerance (eg 25% = 0.25)
	Enable Arbitrage trading engine (y/n)
	Minimum Profit to seek for Arbitrage (eg 10% = 0.10)
	Enable Trend-following trading engine (y/n)
	Polling Interval (in seconds)
	Minimum ticker size for trend following trade decisions
	Maximum ticker age for trend following trade decisions (in minutes)
	Number of ticks used to calculate short Moving Average
	Number of ticks used to calculate short Moving Average Convergence-Divergence
	Number of ticks used to calculate long Moving Average Convergence-Divergence
	Number of MACD values used to calculate MACD Signal Line
	Bid Logic
	Ask Logic
	Which risk algorithm would you like to use? (High Risk or Conservative)
	Trading fee (eg 0.6% = 0.006)
		
If used, the --use-arbitrage and --use-trend command line switches will over-ride their respective configuration file values.
Use of --simulation-mode=false command line switch implies the user agrees with the license terms.

Suggested values
================

The following are just some suggested values:

	Polling interval (in seconds) : 15 to 60 seconds
	Minimum ticker size for trend following trade decisions : Greater than 16
	Maximum ticker age for trend following trade decisions (in minutes) : Greater than 60 minutes
	Number of ticks used to calculate short Moving Average : Minimum ticker size + 1
	Number of ticks used to calculate short Moving Average Convergence-Divergence : 12
	Number of ticks used to calculate long Moving Average Convergence-Divergence: 26
	Number of MACD values used to calculate MACD Signal Line : 9
	Trading fee (eg 0.6% = 0.006) : 0.006

Practical example of Trend-following logic
==========================================

If we have 100 ticks in our ticker.
If of those ticks 60 were up and 40 were down the trendarrow will be +20; the Advance/Decline alogrithm has determined that the market is now trending up.
During that same time there were 75 instances where the bidArrow went up and 25 where it went down, the bidArrow is now +50
If the last trade was 10.25 and the VWAP is 10.20; the Volume articipation algorithm (VWAP Cross) has determined that now is a good time to sell because there is enormous pressure in the market to buy.

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
60 were down, 40 were up the trendArrow is now -20; the Advance/Decline alogrithm has determined that the market is now trending down.
There were 75 instances where the askArrow went down and 25 where it went up.  The askArrow is now -50
The last trade was 10.20, the current VWAP is 10.25; the Volume articipation algorithm (VWAP Cross) has determined that now is a good time to buy because there is enormous pressure in the market to sell.


Our local currency balance is 102.5
(0.2 * 0.5) = 10% of our 102.5 local currency balance
102.5 * 0.1 = 10.25
We now buy 1.025 BTC for 10.25, this gives us a profit of 0.025 BTC

Now imagine that each of these trends continue on average for about 30 minutes before reversing and during that time we place 10 trades essentially identical (since these conditions are really only present for about 10 minutes out of each 30 minute trend), this is a profit of 0.25 BTC during that 30 minute period or 0.5 BTC per hour.
24 * 0.5 BTC = 12 BTC or 12% profit in a day.

This strategy may not work for prolonged trends so it may be benificial to enable one of the slower reacting algorithms like EMA or SMA to determine when we are in a more prolonged trend and to disble trading until market direction changes.
		
Further reading
===============

A detailed discussion about this project may be found at: https://bitcointalk.org/index.php?topic=109831.0
