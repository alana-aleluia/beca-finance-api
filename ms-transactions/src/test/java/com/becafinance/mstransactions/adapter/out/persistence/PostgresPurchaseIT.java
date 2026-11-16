package com.becafinance.mstransactions.adapter.out.persistence;

import com.becafinance.mstransactions.application.dto.*;
import com.becafinance.mstransactions.application.port.out.AccountDataPort;
import com.becafinance.mstransactions.application.usecase.TransactionsUseCase;
import com.becafinance.mstransactions.domain.model.*;
import java.math.BigDecimal;
import java.sql.DriverManager;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "TEST_DATABASE_URL", matches = ".+")
class PostgresPurchaseIT {
    static final AtomicLong IDS = new AtomicLong(1000);
    static final String SCHEMA = "checkpoint_" + UUID.randomUUID().toString().replace("-", "");
    static String url;
    static String username;
    static String password;
    @Autowired TransactionPersistenceAdapter store;
    @Autowired TransactionsUseCase useCase;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean AccountDataPort remote;
    long userId;

    @DynamicPropertySource static void database(DynamicPropertyRegistry properties) throws Exception {
        url = System.getenv("TEST_DATABASE_URL");
        if (!url.matches("jdbc:postgresql://(127\\.0\\.0\\.1|localhost):[0-9]+/[a-zA-Z0-9_]+_test"))
            throw new IllegalArgumentException("Use um banco PostgreSQL local exclusivo terminado em _test");
        username = System.getenv().getOrDefault("TEST_DATABASE_USER", "postgres");
        password = System.getenv().getOrDefault("TEST_DATABASE_PASSWORD", "");
        try (var connection = DriverManager.getConnection(url, username, password); var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + SCHEMA);
        }
        properties.add("spring.datasource.url", () -> url + "?currentSchema=" + SCHEMA);
        properties.add("spring.datasource.username", () -> username);
        properties.add("spring.datasource.password", () -> password);
        properties.add("integrations.ms1-url", () -> "http://127.0.0.1:9");
        properties.add("integrations.mockapi-accounts-url", () -> "http://127.0.0.1:9/accounts");
    }
    @BeforeEach void setup() {
        userId = IDS.incrementAndGet();
        when(remote.load(userId)).thenAnswer(call -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return data(userId, "1000", "500");
        });
    }

    @Test void consecutivePurchasesUseCurrentBalanceAndNeverReinitializeIt() {
        var first = store.process(userId, input("200"), data(userId, "1000", "500"), null, com.becafinance.mstransactions.domain.model.ExchangeQuote.brl());
        var second = store.process(userId, input("200"), data(userId, "9999", "500"), null, com.becafinance.mstransactions.domain.model.ExchangeQuote.brl());
        assertThat(first.balanceAfter()).isEqualByComparingTo("800");
        assertThat(second.balanceBefore()).isEqualByComparingTo("800");
        assertThat(balance(userId)).isEqualByComparingTo("600");
        assertThat(jdbc.queryForObject("select initial_balance from accounts where user_id=?", BigDecimal.class, userId)).isEqualByComparingTo("1000");
        assertThat(countAccounts(userId)).isEqualTo(1);
    }
    @Test void insufficientBalancePersistsRejectedWithoutDebit() {
        store.process(userId, input("400"), data(userId, "500", "500"), null, com.becafinance.mstransactions.domain.model.ExchangeQuote.brl());
        var rejected = store.process(userId, input("200"), data(userId, "500", "500"), null, com.becafinance.mstransactions.domain.model.ExchangeQuote.brl());
        assertThat(rejected.status()).isEqualTo(TransactionStatus.REJECTED);
        assertThat(rejected.rejectionReason()).isEqualTo("INSUFFICIENT_BALANCE");
        assertThat(balance(userId)).isEqualByComparingTo("100");
        assertThat(store.find(userId, rejected.id())).isPresent();
    }
    @Test void limitRejectionPersistsWithoutDebit() {
        var rejected = store.process(userId, input("501"), data(userId, "1000", "500"), null, com.becafinance.mstransactions.domain.model.ExchangeQuote.brl());
        assertThat(rejected.rejectionReason()).isEqualTo("PURCHASE_LIMIT_EXCEEDED");
        assertThat(balance(userId)).isEqualByComparingTo("1000");
        assertThat(store.find(userId, rejected.id())).isPresent();
    }
    @Test void remoteCallsFinishBeforeDatabaseTransaction() {
        assertThat(useCase.process(userId, input("200")).status()).isEqualTo(TransactionStatus.APPROVED);
        verify(remote).load(userId);
    }
    @Test void simultaneousFirstPurchasesCreateOneAccountAndCannotOverspend() throws Exception {
        var barrier = new CyclicBarrier(2);
        when(remote.load(userId)).thenAnswer(call -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            barrier.await(5, TimeUnit.SECONDS);
            return data(userId, "100", "500");
        });
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> useCase.process(userId, input("80")));
            var second = executor.submit(() -> useCase.process(userId, input("80")));
            var results = List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
            assertThat(results).extracting(Transaction::status).containsExactlyInAnyOrder(TransactionStatus.APPROVED, TransactionStatus.REJECTED);
            assertThat(balance(userId)).isEqualByComparingTo("20");
            assertThat(countAccounts(userId)).isEqualTo(1);
            assertThat(jdbc.queryForObject("select count(*) from transactions where user_id=?", Integer.class, userId)).isEqualTo(2);
        }
    }
    @Test void existingAccountWriteLockActuallyBlocksConcurrentPurchase() throws Exception {
        store.process(userId, input("10"), data(userId, "1000", "500"), null, com.becafinance.mstransactions.domain.model.ExchangeQuote.brl());
        try (var connection = DriverManager.getConnection(url + "?currentSchema=" + SCHEMA, username, password);
             var executor = Executors.newSingleThreadExecutor()) {
            connection.setAutoCommit(false);
            try {
                try (var statement = connection.prepareStatement("select id from accounts where user_id=? for update")) {
                    statement.setLong(1, userId); statement.executeQuery().close();
                }
                var future = executor.submit(() -> store.process(userId, input("100"), data(userId, "1000", "500"), null, com.becafinance.mstransactions.domain.model.ExchangeQuote.brl()));
                boolean waiting = false;
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
                while (System.nanoTime() < deadline) {
                    Integer locks = jdbc.queryForObject("select count(*) from pg_stat_activity where datname=current_database() and wait_event_type='Lock' and query like '%accounts%'", Integer.class);
                    if (locks != null && locks > 0) { waiting = true; break; }
                    Thread.sleep(25);
                }
                assertThat(waiting).as("PostgreSQL deve mostrar espera real por bloqueio").isTrue();
                assertThat(future.isDone()).isFalse();
                connection.commit();
                assertThat(future.get(10, TimeUnit.SECONDS).balanceAfter()).isEqualByComparingTo("890");
            } finally { connection.rollback(); }
        }
    }
    @Test void commitFailureRollsBackFlushedBalanceAndTransaction() {
        store.process(userId, input("10"), data(userId, "100", "500"), null, com.becafinance.mstransactions.domain.model.ExchangeQuote.brl());
        jdbc.execute("""
            CREATE OR REPLACE FUNCTION fail_checkpoint_commit() RETURNS trigger LANGUAGE plpgsql AS $$
            BEGIN RAISE EXCEPTION 'Falha técnica simulada no commit'; END $$
            """);
        String triggerName = "fail_commit_" + userId;
        jdbc.execute("CREATE CONSTRAINT TRIGGER " + triggerName + " AFTER INSERT ON transactions DEFERRABLE INITIALLY DEFERRED FOR EACH ROW WHEN (NEW.category = 'FAIL_AT_COMMIT') EXECUTE FUNCTION fail_checkpoint_commit()");
        assertThatThrownBy(() -> store.process(userId,
            new TransactionInput("PURCHASE", new BigDecimal("30"), "BRL", "FAIL_AT_COMMIT"), data(userId, "100", "500"), null, com.becafinance.mstransactions.domain.model.ExchangeQuote.brl()))
            .isInstanceOf(RuntimeException.class);
        assertThat(balance(userId)).isEqualByComparingTo("90");
        assertThat(jdbc.queryForObject("select count(*) from transactions where user_id=?", Integer.class, userId)).isEqualTo(1);
    }
    @Test void failedFirstPurchaseDoesNotLeavePartiallyInitializedAccount() {
        assertThatThrownBy(() -> store.process(userId,
            new TransactionInput("PURCHASE", BigDecimal.ONE, "BRL", "a".repeat(101)), data(userId, "100", "500"), null, com.becafinance.mstransactions.domain.model.ExchangeQuote.brl()))
            .isInstanceOf(RuntimeException.class);
        assertThat(countAccounts(userId)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from transactions where user_id=?", Integer.class, userId)).isZero();
    }
    @Test void ownershipIsEnforcedByDatabaseQuery() {
        var created = store.process(userId, input("20"), data(userId, "1000", "500"), null, com.becafinance.mstransactions.domain.model.ExchangeQuote.brl());
        assertThat(store.find(userId + 10000, created.id())).isEmpty();
        assertThat(store.find(userId, created.id())).isPresent();
    }
    @Test void changedMockIdentityDoesNotDebitOrResetBalance() {
        store.process(userId, input("20"), data(userId, "1000", "500"), null, com.becafinance.mstransactions.domain.model.ExchangeQuote.brl());
        assertThatThrownBy(() -> store.process(userId, input("20"), new AccountData("changed", userId, new BigDecimal("9999"), new BigDecimal("500")), null, com.becafinance.mstransactions.domain.model.ExchangeQuote.brl()))
            .isInstanceOf(com.becafinance.mstransactions.application.exception.DependencyUnavailableException.class);
        assertThat(balance(userId)).isEqualByComparingTo("980");
    }
    private TransactionInput input(String amount) { return new TransactionInput("PURCHASE", new BigDecimal(amount), "BRL", "Food"); }
    private AccountData data(long id, String initial, String limit) { return new AccountData("mock-" + id, id, new BigDecimal(initial), new BigDecimal(limit)); }
    private BigDecimal balance(long id) { return jdbc.queryForObject("select balance from accounts where user_id=?", BigDecimal.class, id); }
    private int countAccounts(long id) { return jdbc.queryForObject("select count(*) from accounts where user_id=?", Integer.class, id); }
    private Transaction operation(String type, String value, long owner, AccountData target) {
        return store.process(owner,new TransactionInput(type,new BigDecimal(value),"BRL","Food",target==null?null:target.userId(),null),
            data(owner,"1000","500"),target,ExchangeQuote.brl());
    }
    @Test void depositAndWithdrawalMaintainCurrentBalance() {
        assertThat(operation("DEPOSIT","1000",userId,null).balanceAfter()).isEqualByComparingTo("2000");
        assertThat(operation("WITHDRAWAL","1500",userId,null).balanceAfter()).isEqualByComparingTo("500");
        var rejected=operation("WITHDRAWAL","501",userId,null);
        assertThat(rejected.status()).isEqualTo(TransactionStatus.REJECTED);
        assertThat(balance(userId)).isEqualByComparingTo("500");
    }
    @Test void transferDebitsAndCreditsAtomicallyAndRetainsRejection() {
        long recipient=IDS.incrementAndGet();
        var target=data(recipient,"100","500");
        var approved=operation("TRANSFER","700",userId,target);
        assertThat(approved.destinationUserId()).isEqualTo(recipient);
        assertThat(balance(userId)).isEqualByComparingTo("300");
        assertThat(balance(recipient)).isEqualByComparingTo("800");
        var rejected=operation("TRANSFER","301",userId,target);
        assertThat(rejected.rejectionReason()).isEqualTo("INSUFFICIENT_BALANCE");
        assertThat(balance(userId)).isEqualByComparingTo("300");
        assertThat(balance(recipient)).isEqualByComparingTo("800");
        assertThat(store.find(recipient,approved.id())).isEmpty();
    }
    @Test void simultaneousOppositeTransfersDoNotDeadlockOrLoseMoney() throws Exception {
        long recipient=IDS.incrementAndGet();
        store.balance(userId,data(userId,"1000","500")); store.balance(recipient,data(recipient,"1000","500"));
        var barrier=new CyclicBarrier(2);
        try(var executor=Executors.newFixedThreadPool(2)) {
            var first=executor.submit(() -> {barrier.await(5,TimeUnit.SECONDS);return operation("TRANSFER","900",userId,data(recipient,"1000","500"));});
            var second=executor.submit(() -> {barrier.await(5,TimeUnit.SECONDS);return operation("TRANSFER","900",recipient,data(userId,"1000","500"));});
            assertThat(first.get(15,TimeUnit.SECONDS).status()).isEqualTo(TransactionStatus.APPROVED);
            assertThat(second.get(15,TimeUnit.SECONDS).status()).isEqualTo(TransactionStatus.APPROVED);
            assertThat(balance(userId)).isEqualByComparingTo("1000"); assertThat(balance(recipient)).isEqualByComparingTo("1000");
        }
    }
    @Test void transferFailureAtCommitRollsBackBothBalances() {
        long recipient=IDS.incrementAndGet();
        store.balance(userId,data(userId,"1000","500")); store.balance(recipient,data(recipient,"1000","500"));
        jdbc.execute("CREATE OR REPLACE FUNCTION fail_transfer_commit() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'Falha simulada'; END $$");
        jdbc.execute("CREATE CONSTRAINT TRIGGER fail_transfer_"+userId+" AFTER INSERT ON transactions DEFERRABLE INITIALLY DEFERRED FOR EACH ROW WHEN (NEW.category='FAIL_TRANSFER') EXECUTE FUNCTION fail_transfer_commit()");
        assertThatThrownBy(() -> store.process(userId,new TransactionInput("TRANSFER",new BigDecimal("200"),"BRL","FAIL_TRANSFER",recipient,null),
            data(userId,"1000","500"),data(recipient,"1000","500"),ExchangeQuote.brl())).isInstanceOf(RuntimeException.class);
        assertThat(balance(userId)).isEqualByComparingTo("1000"); assertThat(balance(recipient)).isEqualByComparingTo("1000");
        assertThat(jdbc.queryForObject("select count(*) from transactions where user_id=?",Integer.class,userId)).isZero();
    }
    @Test void metadataUpdatePreservesFinancialFieldsAndOwnership() {
        var created=operation("PURCHASE","100",userId,null);
        var updated=store.update(userId,created.id(),new MetadataInput("RENAMED","Descrição"));
        assertThat(updated.amountBrl()).isEqualByComparingTo("100"); assertThat(updated.category()).isEqualTo("RENAMED");
        assertThat(balance(userId)).isEqualByComparingTo("900");
        assertThatThrownBy(() -> store.update(userId+10000,created.id(),new MetadataInput("Other",null))).isInstanceOf(com.becafinance.mstransactions.application.exception.TransactionNotFoundException.class);
    }
    @Test void concurrentCancellationRefundsOnlyOnceAndPreservesAudit() throws Exception {
        var created=operation("PURCHASE","100",userId,null);
        try(var executor=Executors.newFixedThreadPool(2)) {
            var first=executor.submit(() -> store.cancel(userId,created.id()));
            var second=executor.submit(() -> store.cancel(userId,created.id()));
            first.get(10,TimeUnit.SECONDS); second.get(10,TimeUnit.SECONDS);
        }
        assertThat(balance(userId)).isEqualByComparingTo("1000");
        assertThat(store.find(userId,created.id()).orElseThrow().cancelledAt()).isNotNull();
        assertThat(jdbc.queryForObject("select count(*) from transaction_cancellations where transaction_id=?",Integer.class,created.id())).isEqualTo(1);
        assertThatThrownBy(() -> store.update(userId,created.id(),new MetadataInput("Changed",null))).isInstanceOf(com.becafinance.mstransactions.application.exception.FinancialConflictException.class);
    }
    @Test void spentDepositCannotBeCancelledIntoNegativeBalance() {
        var deposit=operation("DEPOSIT","1000",userId,null);
        operation("WITHDRAWAL","1900",userId,null);
        assertThatThrownBy(() -> store.cancel(userId,deposit.id())).isInstanceOf(com.becafinance.mstransactions.application.exception.FinancialConflictException.class);
        assertThat(balance(userId)).isEqualByComparingTo("100");
        assertThat(store.find(userId,deposit.id()).orElseThrow().cancelledAt()).isNull();
    }
    @Test void transferCancellationRequiresRecipientFundsAndReversesBothSides() {
        long recipient=IDS.incrementAndGet();
        var transfer=operation("TRANSFER","500",userId,data(recipient,"0","500"));
        store.cancel(userId,transfer.id());
        assertThat(balance(userId)).isEqualByComparingTo("1000"); assertThat(balance(recipient)).isEqualByComparingTo("0");
        var second=operation("TRANSFER","500",userId,data(recipient,"0","500"));
        operation("WITHDRAWAL","500",recipient,null);
        assertThatThrownBy(() -> store.cancel(userId,second.id())).isInstanceOf(com.becafinance.mstransactions.application.exception.FinancialConflictException.class);
        assertThat(balance(userId)).isEqualByComparingTo("500"); assertThat(balance(recipient)).isEqualByComparingTo("0");
    }
    @Test void analysesAndFiltersExcludeRejectionsCancellationsAndOtherUsers() {
        var food=operation("PURCHASE","100",userId,null); operation("PURCHASE","200",userId,null);
        operation("PURCHASE","600",userId,null); operation("DEPOSIT","100",userId,null);
        operation("PURCHASE","300",IDS.incrementAndGet(),null); store.cancel(userId,food.id());
        var filter=new TransactionFilter(null,null,null,null,null,null,null);
        assertThat(store.analyze(userId,filter,"CATEGORY")).containsExactly(new ExpenseSummary("Food",new BigDecimal("200.00")));
        assertThat(store.analyze(userId,filter,"DAY")).hasSize(1);
        assertThat(store.analyze(userId,filter,"MONTH")).hasSize(1);
        assertThat(store.list(userId,filter,false)).hasSize(4).allMatch(t -> t.userId()==userId);
        assertThat(store.list(userId,new TransactionFilter(null,null,"DEPOSIT",null,null,null,null),true)).hasSize(1);
    }
    @Test void foreignCurrencyStoresAppliedRateAndDebitsConvertedAmount() {
        var quote=new ExchangeQuote(new BigDecimal("5.1234"),java.time.LocalDate.parse("2026-11-12"),java.time.OffsetDateTime.parse("2026-11-12T13:00:00-03:00"),"FECHAMENTO PTAX");
        var created=store.process(userId,new TransactionInput("PURCHASE",new BigDecimal("10"),"USD","Food"),data(userId,"1000","500"),null,quote);
        var found=store.find(userId,created.id()).orElseThrow();
        assertThat(found.amountBrl()).isEqualByComparingTo("51.23");
        assertThat(found.exchangeRate()).isEqualByComparingTo("5.1234"); assertThat(found.exchangeDate()).isEqualTo(quote.date());
        assertThat(balance(userId)).isEqualByComparingTo("948.77");
    }
    @Test void v2UpgradesExistingV1DataWithoutChangingValidatedPurchase() throws Exception {
        String schema="upgrade_"+java.util.UUID.randomUUID().toString().replace("-","");
        try(var connection=DriverManager.getConnection(url,username,password);var statement=connection.createStatement()) { statement.execute("CREATE SCHEMA "+schema); }
        org.flywaydb.core.Flyway.configure().dataSource(url,username,password).defaultSchema(schema).target("1").load().migrate();
        try(var connection=DriverManager.getConnection(url+"?currentSchema="+schema,username,password);var statement=connection.createStatement()) {
            statement.execute("INSERT INTO accounts(user_id,mock_account_id,initial_balance,balance) VALUES (2,'1',1000,800)");
            statement.execute("INSERT INTO transactions(account_id,user_id,type,amount,currency,amount_brl,category,purchase_limit,balance_before,balance_after,status,created_at) VALUES (1,2,'PURCHASE',200,'BRL',200,'Food',500,1000,800,'APPROVED',now())");
        }
        org.flywaydb.core.Flyway.configure().dataSource(url,username,password).defaultSchema(schema).load().migrate();
        try(var connection=DriverManager.getConnection(url+"?currentSchema="+schema,username,password);var statement=connection.createStatement();var rows=statement.executeQuery("select a.balance,t.exchange_rate from accounts a join transactions t on t.account_id=a.id")) {
            assertThat(rows.next()).isTrue(); assertThat(rows.getBigDecimal(1)).isEqualByComparingTo("800"); assertThat(rows.getBigDecimal(2)).isEqualByComparingTo("1");
        }
    }
}
