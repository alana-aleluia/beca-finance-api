package com.becafinance.mstransactions.adapter.out.brasilapi;
import com.becafinance.mstransactions.adapter.out.http.HttpClientFactory;
import com.becafinance.mstransactions.application.exception.*;
import com.becafinance.mstransactions.application.port.out.ExchangeRatePort;
import com.becafinance.mstransactions.domain.model.ExchangeQuote;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.*;
import java.time.*;
import java.time.format.*;
import java.time.temporal.ChronoField;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class BrasilApiExchangeAdapter implements ExchangeRatePort {
    private final RestClient client;
    private final Clock clock;
    public BrasilApiExchangeAdapter(@Value("${integrations.brasilapi-url:https://brasilapi.com.br}") String url,
            @Value("${integrations.timeout-seconds:5}") int timeout, Clock clock) {
        client = HttpClientFactory.create(url, timeout); this.clock = clock;
    }
    @Override public ExchangeQuote quote(String currency) {
        if ("BRL".equals(currency)) return ExchangeQuote.brl();
        LocalDate reference = LocalDate.now(clock.withZone(ZoneId.of("America/Sao_Paulo"))).minusDays(1);
        try {
            var response = client.get().uri("/api/cambio/v1/cotacao/{currency}/{date}", currency, reference)
                .retrieve().onStatus(status -> status.value() == 400, (request, result) -> {
                    var body = new String(result.getBody().readNBytes(4096), java.nio.charset.StandardCharsets.UTF_8);
                    if (body.contains("INVALID_CURRENCY_VALUE")) throw new InvalidOperationException("Moeda não suportada pela BrasilAPI");
                    throw new DependencyUnavailableException("Cotação indisponível para a data de referência");
                }).toEntity(DailyQuotes.class);
            var daily = response.getBody();
            if (!response.getStatusCode().is2xxSuccessful() || daily == null || !currency.equals(daily.moeda())
                    || daily.cotacoes() == null || daily.cotacoes().length == 0) throw new IllegalArgumentException();
            LocalDate effective = LocalDate.parse(daily.data());
            if (effective.isAfter(reference) || effective.isBefore(reference.minusDays(14))) throw new IllegalArgumentException();
            var format = new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd HH:mm:ss")
                .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true).toFormatter();
            ExchangeQuote selected = null;
            for (var row : daily.cotacoes()) {
                if (row == null || row.rate() == null || row.rate().signum() <= 0 || row.bulletin() == null
                        || row.bulletin().isBlank() || row.bulletin().length() > 100) throw new IllegalArgumentException();
                var rate = row.rate().setScale(12, RoundingMode.UNNECESSARY);
                if (rate.precision() > 24) throw new IllegalArgumentException();
                var timestamp = LocalDateTime.parse(row.timestamp(), format).atZone(ZoneId.of("America/Sao_Paulo")).toOffsetDateTime();
                if (!timestamp.toLocalDate().equals(effective)) throw new IllegalArgumentException();
                if (selected == null || timestamp.isAfter(selected.quotedAt())) selected = new ExchangeQuote(rate, effective, timestamp, row.bulletin());
            }
            return selected;
        } catch (InvalidOperationException exception) { throw exception; }
        catch (RuntimeException exception) { throw new DependencyUnavailableException("Não foi possível obter cotação válida da BrasilAPI"); }
    }
    public record DailyQuotes(String moeda, String data, QuoteRow[] cotacoes) {}
    public record QuoteRow(@JsonProperty("cotacao_venda") BigDecimal rate,
        @JsonProperty("data_hora_cotacao") String timestamp, @JsonProperty("tipo_boletim") String bulletin) {}
}
