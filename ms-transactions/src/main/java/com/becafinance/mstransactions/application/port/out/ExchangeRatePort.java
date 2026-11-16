package com.becafinance.mstransactions.application.port.out;
import com.becafinance.mstransactions.domain.model.ExchangeQuote;
public interface ExchangeRatePort { ExchangeQuote quote(String currency); }
