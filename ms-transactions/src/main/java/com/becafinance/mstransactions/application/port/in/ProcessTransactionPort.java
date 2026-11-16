package com.becafinance.mstransactions.application.port.in;
import com.becafinance.mstransactions.application.dto.TransactionInput;
import com.becafinance.mstransactions.domain.model.Transaction;
public interface ProcessTransactionPort { Transaction process(long userId, TransactionInput input); }
