package com.becafinance.mstransactions.domain.model;
import java.math.BigDecimal;
import java.time.*;
public record ExchangeQuote(BigDecimal rate, LocalDate date, OffsetDateTime quotedAt, String bulletin) {
    public static ExchangeQuote brl() { return new ExchangeQuote(BigDecimal.ONE, null, null, null); }
}
