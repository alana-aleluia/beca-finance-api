package com.becafinance.mstransactions.domain.model;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class AccountTest {
    private final Account account = new Account(1L, 2L, "mock-1", new BigDecimal("1000.00"), new BigDecimal("800.00"));
    @Test void approvesAndDebitsCurrentBalance() {
        var result = account.evaluate(new BigDecimal("200.00"), new BigDecimal("500.00"));
        assertThat(result.status()).isEqualTo(TransactionStatus.APPROVED);
        assertThat(result.balanceAfter()).isEqualByComparingTo("600.00");
        assertThat(result.rejectionReason()).isNull();
    }
    @Test void rejectsInsufficientBalanceWithoutDebit() {
        var result = account.evaluate(new BigDecimal("900.00"), new BigDecimal("1000.00"));
        assertThat(result.status()).isEqualTo(TransactionStatus.REJECTED);
        assertThat(result.rejectionReason()).isEqualTo("INSUFFICIENT_BALANCE");
        assertThat(result.balanceAfter()).isEqualByComparingTo("800.00");
    }
    @Test void rejectsOverLimitWithoutDebit() {
        var result = account.evaluate(new BigDecimal("501.00"), new BigDecimal("500.00"));
        assertThat(result.rejectionReason()).isEqualTo("PURCHASE_LIMIT_EXCEEDED");
        assertThat(result.balanceAfter()).isEqualByComparingTo("800.00");
    }
    @Test void acceptsExactBalanceAndLimit() {
        var result = account.evaluate(new BigDecimal("800.00"), new BigDecimal("800.00"));
        assertThat(result.status()).isEqualTo(TransactionStatus.APPROVED);
        assertThat(result.balanceAfter()).isEqualByComparingTo("0.00");
    }
    @Test void rejectsNonPositiveAmounts() {
        assertThatThrownBy(() -> account.evaluate(BigDecimal.ZERO, BigDecimal.TEN)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> account.evaluate(BigDecimal.ONE.negate(), BigDecimal.TEN)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void depositCreditsWithoutApplyingPurchaseLimit() {
        assertThat(account.evaluate("DEPOSIT", new BigDecimal("1000"), BigDecimal.ONE).balanceAfter()).isEqualByComparingTo("1800");
    }
    @Test void withdrawalAndTransferUseAvailableBalance() {
        for (String type : new String[]{"WITHDRAWAL", "TRANSFER"}) {
            assertThat(account.evaluate(type, new BigDecimal("800"), BigDecimal.ONE).balanceAfter()).isEqualByComparingTo("0");
            assertThat(account.evaluate(type, new BigDecimal("801"), BigDecimal.ONE).rejectionReason()).isEqualTo("INSUFFICIENT_BALANCE");
        }
    }
    @Test void depositOverflowIsRejectedWithoutChangingBalance() {
        var full = new Account(1L, 2L, "1", BigDecimal.ZERO, new BigDecimal("99999999999999999.99"));
        assertThat(full.evaluate("DEPOSIT", BigDecimal.ONE, BigDecimal.TEN).rejectionReason()).isEqualTo("BALANCE_LIMIT_EXCEEDED");
    }
}
