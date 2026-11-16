package com.becafinance.mstransactions.application.dto;
import com.becafinance.mstransactions.domain.model.Account;
import java.math.BigDecimal;
public record AccountBalance(long id, long userId, BigDecimal initialBalance, BigDecimal balance, String currency, BigDecimal purchaseLimit) {
    public static AccountBalance from(Account account, BigDecimal limit) { return new AccountBalance(account.id(), account.userId(), account.initialBalance(), account.balance(), "BRL", limit); }
}
