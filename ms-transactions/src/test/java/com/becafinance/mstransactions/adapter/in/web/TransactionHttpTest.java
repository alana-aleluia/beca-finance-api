package com.becafinance.mstransactions.adapter.in.web;
import com.becafinance.mstransactions.adapter.in.security.*;
import com.becafinance.mstransactions.application.dto.*;
import com.becafinance.mstransactions.application.exception.*;
import com.becafinance.mstransactions.application.port.out.*;
import com.becafinance.mstransactions.application.usecase.TransactionsUseCase;
import com.becafinance.mstransactions.domain.model.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({TransactionController.class,AccountController.class})
@Import({SecurityConfig.class, RemoteAuthenticationProvider.class, TransactionsUseCase.class})
class TransactionHttpTest {
    @Autowired MockMvc mvc;
    @MockitoBean UserAuthenticationPort users;
    @MockitoBean AccountDataPort accounts;
    @MockitoBean ExchangeRatePort exchange;
    @MockitoBean TransactionStorePort store;
    static final String BODY = "{\"type\":\"PURCHASE\",\"amount\":200.00,\"currency\":\"BRL\",\"category\":\"Food\"}";
    @BeforeEach void setup() { when(users.authenticate("ana@example.com", "test-password")).thenReturn(new UserIdentity(2L, "ana@example.com")); }
    @Test void anonymousPurchaseDoesNotAccessFinancialSystems() throws Exception {
        mvc.perform(post("/transactions").contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isUnauthorized());
        verifyNoInteractions(accounts, store);
    }
    @Test void remoteInvalidCredentialsReturn401() throws Exception {
        when(users.authenticate(anyString(), anyString())).thenThrow(new InvalidCredentialsException());
        mvc.perform(get("/transactions/1").with(httpBasic("unknown@example.com", "wrong"))).andExpect(status().isUnauthorized());
        verifyNoInteractions(accounts, store);
    }
    @Test void ms1OutageReturns503InsteadOfInvalidCredentials() throws Exception {
        when(users.authenticate(anyString(), anyString())).thenThrow(new DependencyUnavailableException("offline"));
        mvc.perform(get("/transactions/1").with(httpBasic("ana@example.com", "test-password"))).andExpect(status().isServiceUnavailable());
        verifyNoInteractions(accounts, store);
    }
    @Test void approvalReturns201WithLocationAndIgnoresForgedOwner() throws Exception {
        var data = new AccountData("1", 2L, new BigDecimal("1000"), new BigDecimal("500"));
        when(accounts.load(2L)).thenReturn(data);
        when(store.process(eq(2L), any(), eq(data), isNull(), any(ExchangeQuote.class))).thenReturn(result(TransactionStatus.APPROVED, null));
        mvc.perform(post("/transactions").with(httpBasic("ana@example.com", "test-password"))
                .contentType(MediaType.APPLICATION_JSON).content(BODY.replace("{", "{\"userId\":999,")))
            .andExpect(status().isCreated()).andExpect(header().string("Location", "/transactions/10"))
            .andExpect(jsonPath("$.status").value("APPROVED")).andExpect(jsonPath("$.userId").value(2))
            .andExpect(jsonPath("$.password").doesNotExist());
        verify(store).process(eq(2L), any(), eq(data), isNull(), any(ExchangeQuote.class));
    }
    @Test void businessRejectionAlsoReturns201() throws Exception {
        var data = new AccountData("1", 2L, BigDecimal.TEN, new BigDecimal("500"));
        when(accounts.load(2L)).thenReturn(data);
        when(store.process(eq(2L), any(), eq(data), isNull(), any(com.becafinance.mstransactions.domain.model.ExchangeQuote.class))).thenReturn(result(TransactionStatus.REJECTED, "INSUFFICIENT_BALANCE"));
        mvc.perform(post("/transactions").with(httpBasic("ana@example.com", "test-password"))
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("REJECTED"))
            .andExpect(jsonPath("$.rejectionReason").value("INSUFFICIENT_BALANCE"));
    }
    @Test void foreignOwnerGets404() throws Exception {
        when(store.find(2L, 10L)).thenReturn(Optional.empty());
        mvc.perform(get("/transactions/10").with(httpBasic("ana@example.com", "test-password"))).andExpect(status().isNotFound());
        verify(store).find(2L, 10L);
    }
    @Test void ownerCanReadTransaction() throws Exception {
        when(store.find(2L, 10L)).thenReturn(Optional.of(result(TransactionStatus.APPROVED, null)));
        mvc.perform(get("/transactions/10").with(httpBasic("ana@example.com", "test-password")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(10));
    }
    @Test void invalidInputsReturn400BeforeCallingMockApi() throws Exception {
        for (String body : new String[] {"{}", BODY.replace("200.00", "0"), BODY.replace("BRL", "XX"), BODY.replace("PURCHASE", "UNKNOWN")})
            mvc.perform(post("/transactions").with(httpBasic("ana@example.com", "test-password"))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        mvc.perform(get("/transactions/0").with(httpBasic("ana@example.com", "test-password"))).andExpect(status().isBadRequest());
        verifyNoInteractions(accounts, store);
    }
    @Test void mockApiFailureDoesNotPersistRejection() throws Exception {
        when(accounts.load(2L)).thenThrow(new DependencyUnavailableException("MockAPI indisponível"));
        mvc.perform(post("/transactions").with(httpBasic("ana@example.com", "test-password"))
            .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isServiceUnavailable());
        verifyNoInteractions(store);
    }
    private Transaction result(TransactionStatus status, String reason) {
        return new Transaction(10L, 2L, 1L, "PURCHASE", new BigDecimal("200"), "BRL", new BigDecimal("200"),
            BigDecimal.ONE, "Food", new BigDecimal("500"), new BigDecimal("1000"),
            status == TransactionStatus.APPROVED ? new BigDecimal("800") : new BigDecimal("1000"), status, reason, OffsetDateTime.now());
    }
    @Test void listAnalysisAndCsvUseAuthenticatedOwner() throws Exception {
        when(store.list(eq(2L),any(),eq(false))).thenReturn(java.util.List.of(result(TransactionStatus.APPROVED,null)));
        when(store.list(eq(2L),any(),eq(true))).thenReturn(java.util.List.of(result(TransactionStatus.APPROVED,null)));
        when(store.analyze(eq(2L),any(),eq("DAY"))).thenReturn(java.util.List.of(new ExpenseSummary("2026-11-13",new BigDecimal("200"))));
        mvc.perform(get("/transactions?userId=999").with(httpBasic("ana@example.com","test-password"))).andExpect(status().isOk()).andExpect(jsonPath("$[0].userId").value(2));
        mvc.perform(get("/transactions/analysis?groupBy=DAY").with(httpBasic("ana@example.com","test-password"))).andExpect(status().isOk()).andExpect(jsonPath("$[0].totalBrl").value(200));
        mvc.perform(get("/transactions/export").with(httpBasic("ana@example.com","test-password"))).andExpect(status().isOk()).andExpect(header().string("Content-Disposition","attachment; filename=transactions.csv"));
    }
    @Test void metadataAndCancellationAreOwnerRestricted() throws Exception {
        when(store.update(eq(2L),eq(99L),any())).thenThrow(new TransactionNotFoundException());
        doThrow(new TransactionNotFoundException()).when(store).cancel(2L,99L);
        mvc.perform(put("/transactions/99").contentType(MediaType.APPLICATION_JSON).content("{\"category\":\"Food\"}").with(httpBasic("ana@example.com","test-password"))).andExpect(status().isNotFound());
        mvc.perform(delete("/transactions/99").with(httpBasic("ana@example.com","test-password"))).andExpect(status().isNotFound());
    }
    @Test void invalidFiltersAndSelfTransferAreRejected() throws Exception {
        mvc.perform(get("/transactions?from=2026-11-13&to=2026-11-12").with(httpBasic("ana@example.com","test-password"))).andExpect(status().isBadRequest());
        mvc.perform(get("/transactions/analysis?groupBy=INVALID").with(httpBasic("ana@example.com","test-password"))).andExpect(status().isBadRequest());
        mvc.perform(post("/transactions").contentType(MediaType.APPLICATION_JSON)
            .content(BODY.replace("PURCHASE","TRANSFER").replace("{","{\"destinationUserId\":2,"))
            .with(httpBasic("ana@example.com","test-password"))).andExpect(status().isBadRequest());
        verifyNoInteractions(accounts,store);
    }
    @Test void balanceCannotBeSelectedByClientUserId() throws Exception {
        var data=new AccountData("1",2L,new BigDecimal("1000"),new BigDecimal("500"));
        when(accounts.load(2L)).thenReturn(data);
        when(store.balance(2L,data)).thenReturn(new Account(1L,2L,"1",new BigDecimal("1000"),new BigDecimal("800")));
        mvc.perform(get("/account/balance?userId=999").with(httpBasic("ana@example.com","test-password")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.balance").value(800)).andExpect(jsonPath("$.userId").value(2));
        verify(accounts).load(2L);
    }
    @Test void transferResponseDoesNotDiscloseRecipientBalance() throws Exception {
        var source=new AccountData("1",2L,new BigDecimal("1000"),new BigDecimal("500"));
        var recipient=new AccountData("2",3L,new BigDecimal("2000"),new BigDecimal("500"));
        when(accounts.load(2L)).thenReturn(source); when(accounts.load(3L)).thenReturn(recipient);
        var transfer=new Transaction(11L,2L,1L,"TRANSFER",new BigDecimal("100"),"BRL",new BigDecimal("100"),BigDecimal.ONE,
            "TRANSFER",new BigDecimal("500"),new BigDecimal("1000"),new BigDecimal("900"),TransactionStatus.APPROVED,null,OffsetDateTime.now(),
            2L,3L,new BigDecimal("2000"),new BigDecimal("2100"),null,null,null,null,null,null);
        when(store.process(eq(2L),any(),eq(source),eq(recipient),any())).thenReturn(transfer);
        mvc.perform(post("/transactions").with(httpBasic("ana@example.com","test-password")).contentType(MediaType.APPLICATION_JSON)
            .content("{\"type\":\"TRANSFER\",\"amount\":100,\"currency\":\"BRL\",\"category\":\"TRANSFER\",\"destinationUserId\":3}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.destinationUserId").value(3))
            .andExpect(jsonPath("$.destinationBalanceBefore").doesNotExist()).andExpect(jsonPath("$.destinationBalanceAfter").doesNotExist())
            .andExpect(jsonPath("$.destinationAccountId").doesNotExist());
    }
}
