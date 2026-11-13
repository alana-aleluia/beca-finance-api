package com.becafinance.mstransactions.adapter.out.persistence.entity;
import com.becafinance.mstransactions.domain.model.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity @Table(name = "transactions")
public class TransactionJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "account_id", nullable = false) private long accountId;
    @Column(name = "user_id", nullable = false) private long userId;
    @Column(nullable = false, length = 20) private String type;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 3) private String currency;
    @Column(name = "amount_brl", nullable = false, precision = 19, scale = 2) private BigDecimal amountBrl;
    @Column(nullable = false, length = 100) private String category;
    @Column(name = "purchase_limit", nullable = false, precision = 19, scale = 2) private BigDecimal purchaseLimit;
    @Column(name = "balance_before", nullable = false, precision = 19, scale = 2) private BigDecimal balanceBefore;
    @Column(name = "balance_after", nullable = false, precision = 19, scale = 2) private BigDecimal balanceAfter;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private TransactionStatus status;
    @Column(name = "rejection_reason", length = 40) private String rejectionReason;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "exchange_rate", nullable = false, precision = 24, scale = 12) private BigDecimal exchangeRate;
    @Column(name = "exchange_date") private java.time.LocalDate exchangeDate;
    @Column(name = "quoted_at") private OffsetDateTime quotedAt;
    @Column(name = "exchange_bulletin", length = 100) private String exchangeBulletin;
    @Column(name = "destination_account_id") private Long destinationAccountId;
    @Column(name = "destination_user_id") private Long destinationUserId;
    @Column(name = "destination_balance_before", precision = 19, scale = 2) private BigDecimal destinationBalanceBefore;
    @Column(name = "destination_balance_after", precision = 19, scale = 2) private BigDecimal destinationBalanceAfter;
    @Column(length = 500) private String description;
    @Column(name = "cancelled_at") private OffsetDateTime cancelledAt;
    @Column(name = "updated_at") private OffsetDateTime updatedAt;
    protected TransactionJpaEntity() {}
    public TransactionJpaEntity(Transaction transaction) {
        accountId = transaction.accountId(); userId = transaction.userId(); type = transaction.type();
        amount = transaction.amount(); currency = transaction.currency(); amountBrl = transaction.amountBrl();
        category = transaction.category(); purchaseLimit = transaction.purchaseLimit(); balanceBefore = transaction.balanceBefore();
        balanceAfter = transaction.balanceAfter(); status = transaction.status(); rejectionReason = transaction.rejectionReason();
        createdAt = transaction.createdAt();
        exchangeRate = transaction.exchangeRate(); exchangeDate = transaction.exchangeDate(); quotedAt = transaction.quotedAt();
        exchangeBulletin = transaction.exchangeBulletin(); destinationAccountId = transaction.destinationAccountId();
        destinationUserId = transaction.destinationUserId(); destinationBalanceBefore = transaction.destinationBalanceBefore();
        destinationBalanceAfter = transaction.destinationBalanceAfter(); description = transaction.description();
        cancelledAt = transaction.cancelledAt(); updatedAt = transaction.updatedAt();
    }
    public Transaction toDomain() {
        return new Transaction(id, userId, accountId, type, amount, currency, amountBrl, exchangeRate,
            category, purchaseLimit, balanceBefore, balanceAfter, status, rejectionReason, createdAt,
            destinationAccountId, destinationUserId, destinationBalanceBefore, destinationBalanceAfter, exchangeDate,
            quotedAt, exchangeBulletin, description, cancelledAt, updatedAt);
    }
    public void metadata(String category, String description) { this.category = category; this.description = description; updatedAt = OffsetDateTime.now(java.time.ZoneOffset.UTC); }
    public void cancel(OffsetDateTime time) { cancelledAt = time; updatedAt = time; }
}
