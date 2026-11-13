package com.becafinance.mstransactions.application.port.in;
import com.becafinance.mstransactions.application.dto.*;
import com.becafinance.mstransactions.domain.model.*;
import java.util.List;
public interface ManageTransactionsPort {
    List<Transaction> list(long userId, TransactionFilter filter);
    Transaction update(long userId, long id, MetadataInput input);
    void cancel(long userId, long id);
    List<ExpenseSummary> analyze(long userId, TransactionFilter filter, String groupBy);
    List<Transaction> export(long userId, TransactionFilter filter);
    AccountBalance balance(long userId);
}
