package com.becafinance.mstransactions.application.usecase;
import com.becafinance.mstransactions.application.dto.*;
import com.becafinance.mstransactions.application.exception.*;
import com.becafinance.mstransactions.application.port.out.*;
import jakarta.validation.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionsUseCaseTest {
    @Mock AccountDataPort accounts;
    @Mock TransactionStorePort store;
    static ValidatorFactory factory;
    TransactionsUseCase useCase;
    @BeforeAll static void init() { factory = Validation.buildDefaultValidatorFactory(); }
    @AfterAll static void close() { factory.close(); }
    @BeforeEach void setup() { useCase = new TransactionsUseCase(accounts, store, factory.getValidator(), mock(ExchangeRatePort.class)); }
    @Test void obtainsRemoteDataBeforeAtomicOperation() {
        var input = input("200.00", "BRL");
        var data = new AccountData("1", 2L, new BigDecimal("1000.00"), new BigDecimal("500.00"));
        when(accounts.load(2L)).thenReturn(data);
        useCase.process(2L, input);
        var order = inOrder(accounts, store);
        order.verify(accounts).load(2L);
        order.verify(store).process(2L, input, data, null, com.becafinance.mstransactions.domain.model.ExchangeQuote.brl());
    }
    @Test void invalidInputsDoNotAccessExternalSystems() {
        for (var input : new TransactionInput[] {input("0.00", "BRL"), input("-1", "BRL"), input("1.001", "BRL"),
                input("200", "XX"), new TransactionInput("UNKNOWN", BigDecimal.ONE, "BRL", "Food"),
                new TransactionInput("PURCHASE", BigDecimal.ONE, "BRL", " ")}) {
            assertThatThrownBy(() -> useCase.process(2L, input)).isInstanceOf(ConstraintViolationException.class);
        }
        verifyNoInteractions(accounts, store);
    }
    @Test void dependencyFailureDoesNotCreateFinancialRejection() {
        when(accounts.load(2L)).thenThrow(new DependencyUnavailableException("unavailable"));
        assertThatThrownBy(() -> useCase.process(2L, input("200", "BRL"))).isInstanceOf(DependencyUnavailableException.class);
        verifyNoInteractions(store);
    }
    @Test void mismatchedOwnerCannotInitializeAccount() {
        when(accounts.load(2L)).thenReturn(new AccountData("1", 3L, BigDecimal.TEN, BigDecimal.TEN));
        assertThatThrownBy(() -> useCase.process(2L, input("1", "BRL"))).isInstanceOf(DependencyUnavailableException.class);
        verifyNoInteractions(store);
    }
    @Test void lookupAlwaysIncludesOwnerAndHidesOtherUsersTransactions() {
        when(store.find(2L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.find(2L, 1L)).isInstanceOf(TransactionNotFoundException.class);
        verify(store).find(2L, 1L);
    }
    private TransactionInput input(String amount, String currency) { return new TransactionInput("PURCHASE", new BigDecimal(amount), currency, "Food"); }
}
