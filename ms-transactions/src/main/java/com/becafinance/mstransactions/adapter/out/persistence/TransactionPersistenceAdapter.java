package com.becafinance.mstransactions.adapter.out.persistence;

import com.becafinance.mstransactions.adapter.out.persistence.entity.*;
import com.becafinance.mstransactions.adapter.out.persistence.repository.*;
import com.becafinance.mstransactions.application.dto.*;
import com.becafinance.mstransactions.application.exception.*;
import com.becafinance.mstransactions.application.port.out.TransactionStorePort;
import com.becafinance.mstransactions.domain.model.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.*;

@Repository
public class TransactionPersistenceAdapter implements TransactionStorePort {
    private static final BigDecimal MAX_BALANCE = new BigDecimal("99999999999999999.99");
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private final EntityManager entityManager;
    private final AccountJpaRepository accounts;
    private final TransactionJpaRepository transactions;
    public TransactionPersistenceAdapter(EntityManager entityManager, AccountJpaRepository accounts, TransactionJpaRepository transactions) {
        this.entityManager = entityManager; this.accounts = accounts; this.transactions = transactions;
    }
    // Todas as integrações HTTP terminam antes desta fronteira transacional.
    @Override @Transactional(isolation = Isolation.READ_COMMITTED, timeout = 10)
    public Transaction process(long userId, TransactionInput input, AccountData source, AccountData destination, ExchangeQuote quote) {
        if (source.userId() != userId || ("TRANSFER".equals(input.type()) && (destination == null || destination.userId() == userId)))
            throw new InvalidOperationException("Contas incompatíveis com a operação");
        var seeds = new ArrayList<AccountData>(); seeds.add(source); if (destination != null) seeds.add(destination);
        var locked = initializeAndLock(seeds);
        var sourceEntity = locked.get(userId);
        var account = sourceEntity.toDomain();
        var destinationEntity = destination == null ? null : locked.get(destination.userId());
        var destinationAccount = destinationEntity == null ? null : destinationEntity.toDomain();
        var amountBrl = input.amount().multiply(quote.rate()).setScale(2, RoundingMode.HALF_EVEN);
        var decision = account.evaluate(input.type(), amountBrl, source.purchaseLimit());
        if (decision.status() == TransactionStatus.APPROVED && destinationAccount != null
                && destinationAccount.balance().add(amountBrl).compareTo(MAX_BALANCE) > 0)
            decision = new PurchaseDecision(TransactionStatus.REJECTED, "DESTINATION_BALANCE_LIMIT_EXCEEDED", account.balance());
        BigDecimal destinationAfter = destinationAccount == null ? null : destinationAccount.balance();
        if (decision.status() == TransactionStatus.APPROVED) {
            sourceEntity.changeBalance(decision.balanceAfter());
            if (destinationEntity != null) {
                destinationAfter = destinationAccount.balance().add(amountBrl);
                destinationEntity.changeBalance(destinationAfter);
            }
        }
        var transaction = new Transaction(null, userId, account.id(), input.type(), input.amount(), input.currency(),
            amountBrl, quote.rate(), input.category(), source.purchaseLimit(), account.balance(), decision.balanceAfter(),
            decision.status(), decision.rejectionReason(), OffsetDateTime.now(ZoneOffset.UTC),
            destinationAccount == null ? null : destinationAccount.id(), destination == null ? null : destination.userId(),
            destinationAccount == null ? null : destinationAccount.balance(), destinationAfter,
            quote.date(), quote.quotedAt(), quote.bulletin(), input.description(), null, null);
        return transactions.saveAndFlush(new TransactionJpaEntity(transaction)).toDomain();
    }
    private Map<Long, AccountJpaEntity> initializeAndLock(List<AccountData> seeds) {
        timeout();
        // A ordem de inicialização e a unicidade protegem contas ausentes em transferências concorrentes.
        for (var seed : seeds.stream().sorted(Comparator.comparingLong(AccountData::userId)).toList()) {
            entityManager.createNativeQuery("""
                INSERT INTO accounts(user_id,mock_account_id,initial_balance,balance)
                VALUES (:userId,:mockId,:initial,:initial) ON CONFLICT (user_id) DO NOTHING
                """).setParameter("userId", seed.userId()).setParameter("mockId", seed.id())
                .setParameter("initial", seed.initialBalance()).executeUpdate();
        }
        var result = lockAccounts(seeds.stream().map(AccountData::userId).toList());
        for (var seed : seeds) if (!result.get(seed.userId()).toDomain().mockAccountId().equals(seed.id()))
            throw new DependencyUnavailableException("A origem da conta foi alterada; processamento interrompido");
        return result;
    }
    private Map<Long, AccountJpaEntity> lockAccounts(List<Long> users) {
        // A projeção escalar não carrega saldos no contexto JPA antes de obter o bloqueio.
        var ordered = new TreeMap<Long, Long>();
        for (long user : users) {
            Number id = (Number) entityManager.createNativeQuery("SELECT id FROM accounts WHERE user_id=:user")
                .setParameter("user", user).getSingleResult();
            ordered.put(id.longValue(), user);
        }
        var result = new HashMap<Long, AccountJpaEntity>();
        for (long user : ordered.values()) result.put(user, accounts.lockByUserId(user).orElseThrow());
        return result;
    }
    private void timeout() { entityManager.createNativeQuery("SET LOCAL lock_timeout = '5s'").executeUpdate(); }
    @Override @Transactional(readOnly = true)
    public Optional<Transaction> find(long userId, long id) { return transactions.findByIdAndUserId(id, userId).map(TransactionJpaEntity::toDomain); }
    @Override @Transactional(readOnly = true)
    public List<Transaction> list(long userId, TransactionFilter filter, boolean export) {
        int size = export ? 10001 : filter.pageSize();
        var rows = transactions.findAll(specification(userId, filter), PageRequest.of(export ? 0 : filter.pageNumber(), size, Sort.by("id").descending())).getContent();
        if (export && rows.size() > 10000) throw new InvalidOperationException("Limite de 10000 registros no CSV; restrinja o período");
        return rows.stream().map(TransactionJpaEntity::toDomain).toList();
    }
    private Specification<TransactionJpaEntity> specification(long userId, TransactionFilter filter) {
        return (root, query, cb) -> {
            var conditions = new ArrayList<Predicate>(); conditions.add(cb.equal(root.get("userId"), userId));
            if (filter.from() != null) conditions.add(cb.greaterThanOrEqualTo(root.get("createdAt"), start(filter.from())));
            if (filter.to() != null) conditions.add(cb.lessThan(root.get("createdAt"), start(filter.to().plusDays(1))));
            if (filter.type() != null) conditions.add(cb.equal(root.get("type"), filter.type()));
            if (filter.status() != null) conditions.add(cb.equal(root.get("status"), TransactionStatus.valueOf(filter.status())));
            if (filter.category() != null) conditions.add(cb.equal(root.get("category"), filter.category()));
            return cb.and(conditions.toArray(Predicate[]::new));
        };
    }
    private OffsetDateTime start(LocalDate date) { return date.atStartOfDay(ZONE).toOffsetDateTime(); }
    @Override @Transactional(timeout = 10)
    public Transaction update(long userId, long id, MetadataInput input) {
        timeout();
        var entity = transactions.lockOwned(id, userId).orElseThrow(TransactionNotFoundException::new);
        if (entity.toDomain().cancelledAt() != null) throw new FinancialConflictException("Transação cancelada não pode ser alterada");
        entity.metadata(input.category(), input.description());
        return transactions.saveAndFlush(entity).toDomain();
    }
    @Override @Transactional(isolation = Isolation.READ_COMMITTED, timeout = 10)
    public void cancel(long userId, long id) {
        timeout();
        var entity = transactions.lockOwned(id, userId).orElseThrow(TransactionNotFoundException::new);
        var original = entity.toDomain();
        if (original.cancelledAt() != null) return;
        var users = new ArrayList<Long>(); users.add(userId); if (original.destinationUserId() != null) users.add(original.destinationUserId());
        var locked = lockAccounts(users);
        var source = locked.get(userId);
        var before = source.toDomain().balance();
        var after = before;
        var destination = original.destinationUserId() == null ? null : locked.get(original.destinationUserId());
        BigDecimal destinationBefore = destination == null ? null : destination.toDomain().balance();
        BigDecimal destinationAfter = destinationBefore;
        if (original.status() == TransactionStatus.APPROVED) {
            after = "DEPOSIT".equals(original.type()) ? before.subtract(original.amountBrl()) : before.add(original.amountBrl());
            if (destination != null) destinationAfter = destinationBefore.subtract(original.amountBrl());
            if (after.signum() < 0 || after.compareTo(MAX_BALANCE) > 0 || (destinationAfter != null && destinationAfter.signum() < 0))
                throw new FinancialConflictException("Estorno recusado: saldo insuficiente ou limite monetário excedido");
            source.changeBalance(after); if (destination != null) destination.changeBalance(destinationAfter);
        }
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        entityManager.createNativeQuery("""
            INSERT INTO transaction_cancellations(transaction_id,cancelled_at,source_balance_before,source_balance_after,destination_balance_before,destination_balance_after)
            VALUES (:id,:now,:before,:after,:destBefore,:destAfter)
            """).setParameter("id", id).setParameter("now", now).setParameter("before", before).setParameter("after", after)
            .setParameter("destBefore", destinationBefore).setParameter("destAfter", destinationAfter).executeUpdate();
        entity.cancel(now); entityManager.flush();
    }
    @Override @Transactional(readOnly = true)
    public List<ExpenseSummary> analyze(long userId, TransactionFilter filter, String groupBy) {
        String group = switch (groupBy) {
            case "CATEGORY" -> "category";
            case "DAY" -> "(created_at AT TIME ZONE 'America/Sao_Paulo')::date::text";
            case "MONTH" -> "to_char(created_at AT TIME ZONE 'America/Sao_Paulo','YYYY-MM')";
            default -> throw new InvalidOperationException("Agrupamento inválido");
        };
        var sql = new StringBuilder("SELECT " + group + ",sum(amount_brl) FROM transactions WHERE user_id=:user AND type='PURCHASE' AND status='APPROVED' AND cancelled_at IS NULL");
        if (filter.from() != null) sql.append(" AND created_at>=:from");
        if (filter.to() != null) sql.append(" AND created_at<:to");
        if (filter.type() != null) sql.append(" AND type=:type");
        if (filter.status() != null) sql.append(" AND status=:status");
        if (filter.category() != null) sql.append(" AND category=:category");
        sql.append(" GROUP BY 1 ORDER BY 1");
        var query = entityManager.createNativeQuery(sql.toString()).setParameter("user", userId);
        if (filter.from() != null) query.setParameter("from", start(filter.from()));
        if (filter.to() != null) query.setParameter("to", start(filter.to().plusDays(1)));
        if (filter.type() != null) query.setParameter("type", filter.type());
        if (filter.status() != null) query.setParameter("status", filter.status());
        if (filter.category() != null) query.setParameter("category", filter.category());
        @SuppressWarnings("unchecked") List<Object[]> rows = query.getResultList();
        return rows.stream().map(row -> new ExpenseSummary(row[0].toString(), (BigDecimal) row[1])).toList();
    }
    @Override @Transactional(isolation = Isolation.READ_COMMITTED, timeout = 10)
    public Account balance(long userId, AccountData data) {
        if (data.userId() != userId) throw new DependencyUnavailableException("Conta externa incompatível");
        return initializeAndLock(List.of(data)).get(userId).toDomain();
    }
}
