package com.becafinance.mstransactions.application.port.in;
import com.becafinance.mstransactions.domain.model.Transaction;
public interface FindTransactionPort { Transaction find(long userId, long id); }
