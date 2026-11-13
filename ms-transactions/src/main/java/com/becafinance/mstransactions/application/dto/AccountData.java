package com.becafinance.mstransactions.application.dto;
import java.math.BigDecimal;
public record AccountData(String id, long userId, BigDecimal initialBalance, BigDecimal purchaseLimit) {}
