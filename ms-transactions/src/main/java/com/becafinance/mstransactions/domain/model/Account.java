package com.becafinance.mstransactions.domain.model;

import java.math.BigDecimal;

public record Account(Long id, long userId, String mockAccountId, BigDecimal initialBalance, BigDecimal balance) {
    public PurchaseDecision evaluate(BigDecimal amount, BigDecimal purchaseLimit) {
        if (amount.signum() <= 0) throw new IllegalArgumentException("Valor deve ser positivo");
        if (amount.compareTo(purchaseLimit) > 0) return new PurchaseDecision(TransactionStatus.REJECTED, "PURCHASE_LIMIT_EXCEEDED", balance);
        if (amount.compareTo(balance) > 0) return new PurchaseDecision(TransactionStatus.REJECTED, "INSUFFICIENT_BALANCE", balance);
        return new PurchaseDecision(TransactionStatus.APPROVED, null, balance.subtract(amount));
    }
    public PurchaseDecision evaluate(String type, BigDecimal amount, BigDecimal purchaseLimit) {
        if ("PURCHASE".equals(type)) return evaluate(amount, purchaseLimit);
        if (amount.signum() <= 0) throw new IllegalArgumentException("Valor deve ser positivo");
        if ("DEPOSIT".equals(type)) {
            var after = balance.add(amount);
            if (after.compareTo(new BigDecimal("99999999999999999.99")) > 0)
                return new PurchaseDecision(TransactionStatus.REJECTED, "BALANCE_LIMIT_EXCEEDED", balance);
            return new PurchaseDecision(TransactionStatus.APPROVED, null, after);
        }
        if (!("WITHDRAWAL".equals(type) || "TRANSFER".equals(type))) throw new IllegalArgumentException("Tipo inválido");
        if (amount.compareTo(balance) > 0) return new PurchaseDecision(TransactionStatus.REJECTED, "INSUFFICIENT_BALANCE", balance);
        return new PurchaseDecision(TransactionStatus.APPROVED, null, balance.subtract(amount));
    }
}
