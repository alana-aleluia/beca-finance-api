package com.becafinance.mstransactions.adapter.out.mockapi;
import com.becafinance.mstransactions.adapter.out.http.HttpClientFactory;
import com.becafinance.mstransactions.application.dto.AccountData;
import com.becafinance.mstransactions.application.exception.DependencyUnavailableException;
import com.becafinance.mstransactions.application.port.out.AccountDataPort;
import java.math.BigDecimal;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class MockApiAccountAdapter implements AccountDataPort {
    private final RestClient client;
    public MockApiAccountAdapter(@Value("${integrations.mockapi-accounts-url}") String url,
            @Value("${integrations.timeout-seconds:5}") int timeout) { client = HttpClientFactory.create(url, timeout); }

    @Override public AccountData load(long userId) {
        try {
            var response = client.get().uri(builder -> builder.queryParam("userId", userId).build())
                .retrieve().toEntity(AccountResponse[].class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null)
                throw new IllegalArgumentException();
            // O filtro remoto pode ser aproximado; exigimos correspondência exata e uma única conta.
            var matches = Arrays.stream(response.getBody()).filter(row -> row != null && row.userId() != null && row.userId() == userId).toList();
            if (matches.size() != 1) throw new IllegalArgumentException();
            var row = matches.getFirst();
            if (row.id() == null || row.id().isBlank() || row.id().length() > 100 || !"BRL".equals(row.currency()))
                throw new IllegalArgumentException();
            return new AccountData(row.id(), userId, money(row.initialBalance()), money(row.purchaseLimit()));
        } catch (RuntimeException exception) {
            throw new DependencyUnavailableException("Conta indisponível ou resposta inválida da MockAPI");
        }
    }
    private BigDecimal money(String value) {
        var amount = new BigDecimal(value).setScale(2, java.math.RoundingMode.UNNECESSARY);
        if (amount.signum() < 0 || amount.precision() > 19) throw new IllegalArgumentException();
        return amount;
    }
    public record AccountResponse(String id, Long userId, String initialBalance, String currency, String purchaseLimit) {}
}
