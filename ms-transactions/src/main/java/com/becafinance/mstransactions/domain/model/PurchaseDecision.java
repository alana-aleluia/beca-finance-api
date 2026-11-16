package com.becafinance.mstransactions.domain.model;
import java.math.BigDecimal;
public record PurchaseDecision(TransactionStatus status, String rejectionReason, BigDecimal balanceAfter) {}
