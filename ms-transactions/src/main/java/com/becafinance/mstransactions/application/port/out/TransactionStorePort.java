package com.becafinance.mstransactions.application.port.out;
import com.becafinance.mstransactions.application.dto.*;
import com.becafinance.mstransactions.domain.model.Transaction;
import java.util.Optional;
import java.util.List;
import com.becafinance.mstransactions.domain.model.*;
public interface TransactionStorePort {
    Transaction process(long userId, TransactionInput input, AccountData data, AccountData destination, ExchangeQuote quote);
    Optional<Transaction> find(long userId, long id);
    List<Transaction> list(long userId, TransactionFilter filter, boolean export);
    Transaction update(long userId, long id, MetadataInput input);
    void cancel(long userId, long id);
    List<ExpenseSummary> analyze(long userId, TransactionFilter filter, String groupBy);
    Account balance(long userId, AccountData initialData);
}
