package com.becafinance.mstransactions.adapter.in.web;
import com.becafinance.mstransactions.domain.model.*;
import java.math.BigDecimal;
import java.time.*;

// Os saldos do destinatário são internos à auditoria e nunca fazem parte da resposta ao remetente.
public record TransactionResponse(Long id, long userId, long accountId, String type, BigDecimal amount,
    String currency, BigDecimal amountBrl, BigDecimal exchangeRate, String category, BigDecimal purchaseLimit,
    BigDecimal balanceBefore, BigDecimal balanceAfter, TransactionStatus status, String rejectionReason,
    OffsetDateTime createdAt, Long destinationUserId, LocalDate exchangeDate, OffsetDateTime quotedAt,
    String exchangeBulletin, String description, OffsetDateTime cancelledAt, OffsetDateTime updatedAt) {
    public static TransactionResponse from(Transaction t) {
        return new TransactionResponse(t.id(),t.userId(),t.accountId(),t.type(),t.amount(),t.currency(),t.amountBrl(),
            t.exchangeRate(),t.category(),t.purchaseLimit(),t.balanceBefore(),t.balanceAfter(),t.status(),t.rejectionReason(),
            t.createdAt(),t.destinationUserId(),t.exchangeDate(),t.quotedAt(),t.exchangeBulletin(),t.description(),t.cancelledAt(),t.updatedAt());
    }
}
