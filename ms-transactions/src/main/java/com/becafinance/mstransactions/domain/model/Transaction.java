package com.becafinance.mstransactions.domain.model;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record Transaction(Long id, long userId, long accountId, String type, BigDecimal amount,
    String currency, BigDecimal amountBrl, BigDecimal exchangeRate, String category,
    BigDecimal purchaseLimit, BigDecimal balanceBefore, BigDecimal balanceAfter,
    TransactionStatus status, String rejectionReason, OffsetDateTime createdAt,
    Long destinationAccountId, Long destinationUserId, BigDecimal destinationBalanceBefore, BigDecimal destinationBalanceAfter,
    java.time.LocalDate exchangeDate, OffsetDateTime quotedAt, String exchangeBulletin,
    String description, OffsetDateTime cancelledAt, OffsetDateTime updatedAt) {
    public Transaction(Long id, long userId, long accountId, String type, BigDecimal amount, String currency,
            BigDecimal amountBrl, BigDecimal exchangeRate, String category, BigDecimal purchaseLimit,
            BigDecimal balanceBefore, BigDecimal balanceAfter, TransactionStatus status, String rejectionReason, OffsetDateTime createdAt) {
        this(id, userId, accountId, type, amount, currency, amountBrl, exchangeRate, category, purchaseLimit,
            balanceBefore, balanceAfter, status, rejectionReason, createdAt, null, null, null, null, null, null, null, null, null, null);
    }
}
