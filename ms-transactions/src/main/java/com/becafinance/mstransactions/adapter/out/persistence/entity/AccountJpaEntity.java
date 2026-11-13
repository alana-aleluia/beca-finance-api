package com.becafinance.mstransactions.adapter.out.persistence.entity;
import com.becafinance.mstransactions.domain.model.Account;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity @Table(name = "accounts")
public class AccountJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private long userId;
    @Column(name = "mock_account_id", nullable = false, length = 100) private String mockAccountId;
    @Column(name = "initial_balance", nullable = false, precision = 19, scale = 2) private BigDecimal initialBalance;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal balance;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;
    protected AccountJpaEntity() {}
    public Account toDomain() { return new Account(id, userId, mockAccountId, initialBalance, balance); }
    public void changeBalance(BigDecimal value) { balance = value; }
}
