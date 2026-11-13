package com.becafinance.mstransactions.application.usecase;

import com.becafinance.mstransactions.application.dto.*;
import com.becafinance.mstransactions.application.exception.*;
import com.becafinance.mstransactions.application.port.in.*;
import com.becafinance.mstransactions.application.port.out.*;
import com.becafinance.mstransactions.domain.model.*;
import java.util.List;
import jakarta.validation.*;
import org.springframework.stereotype.Service;

@Service
public class TransactionsUseCase implements ProcessTransactionPort, FindTransactionPort, ManageTransactionsPort {
    private final AccountDataPort accounts;
    private final TransactionStorePort store;
    private final Validator validator;
    private final ExchangeRatePort exchange;
    public TransactionsUseCase(AccountDataPort accounts, TransactionStorePort store, Validator validator, ExchangeRatePort exchange) {
        this.accounts = accounts; this.store = store; this.validator = validator;
        this.exchange = exchange;
    }
    @Override public Transaction process(long userId, TransactionInput input) {
        validate(input);
        if (input.destinationUserId() != null && input.destinationUserId() == userId)
            throw new InvalidOperationException("A conta de destino deve ser diferente da origem");
        var data = accounts.load(userId);
        if (data.userId() != userId) throw new DependencyUnavailableException("Conta externa incompatível");
        AccountData destination = input.destinationUserId() == null ? null : accounts.load(input.destinationUserId());
        if (destination != null && destination.userId() != input.destinationUserId())
            throw new DependencyUnavailableException("Conta de destino incompatível");
        var quote = "BRL".equals(input.currency()) ? ExchangeQuote.brl() : exchange.quote(input.currency());
        var converted = input.amount().multiply(quote.rate()).setScale(2, java.math.RoundingMode.HALF_EVEN);
        if (converted.signum() <= 0 || converted.precision() > 19) throw new InvalidOperationException("Valor convertido fora dos limites monetários");
        return store.process(userId, input, data, destination, quote);
    }
    @Override public Transaction find(long userId, long id) {
        return store.find(userId, id).orElseThrow(TransactionNotFoundException::new);
    }
    @Override public List<Transaction> list(long userId, TransactionFilter filter) { validate(filter); return store.list(userId, filter, false); }
    @Override public Transaction update(long userId, long id, MetadataInput input) { validate(input); return store.update(userId, id, input); }
    @Override public void cancel(long userId, long id) { store.cancel(userId, id); }
    @Override public List<ExpenseSummary> analyze(long userId, TransactionFilter filter, String groupBy) {
        validate(filter);
        if (!List.of("CATEGORY", "DAY", "MONTH").contains(groupBy)) throw new InvalidOperationException("Agrupamento inválido");
        return store.analyze(userId, filter, groupBy);
    }
    @Override public List<Transaction> export(long userId, TransactionFilter filter) { validate(filter); return store.list(userId, filter, true); }
    @Override public AccountBalance balance(long userId) {
        var data = accounts.load(userId);
        return AccountBalance.from(store.balance(userId, data), data.purchaseLimit());
    }
    private void validate(Object input) {
        var violations = validator.validate(input);
        if (!violations.isEmpty()) throw new ConstraintViolationException(violations);
    }
}
