package com.becafinance.mstransactions.adapter.out.http;
import com.becafinance.mstransactions.adapter.out.brasilapi.BrasilApiExchangeAdapter;
import com.becafinance.mstransactions.application.exception.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.*;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

class BrasilApiExchangeTest {
    HttpServer server;
    volatile String body;
    volatile int status;
    volatile String path;
    BrasilApiExchangeAdapter adapter;
    static final String QUOTES = """
        {"moeda":"USD","data":"2026-11-12","cotacoes":[
          {"cotacao_venda":5.1,"data_hora_cotacao":"2026-11-12 10:00:00.123","tipo_boletim":"ABERTURA"},
          {"cotacao_venda":5.2,"data_hora_cotacao":"2026-11-12 13:00:00.123456","tipo_boletim":"FECHAMENTO PTAX"}]}
        """;
    @BeforeEach void setup() throws Exception {
        body = QUOTES; status = 200;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/", exchange -> {
            path = exchange.getRequestURI().getPath();
            assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isNull();
            var bytes=body.getBytes(StandardCharsets.UTF_8); exchange.getResponseHeaders().set("Content-Type","application/json");
            exchange.sendResponseHeaders(status,bytes.length); exchange.getResponseBody().write(bytes); exchange.close();
        });
        server.start(); adapter = new BrasilApiExchangeAdapter("http://127.0.0.1:"+server.getAddress().getPort(),2,
            Clock.fixed(Instant.parse("2026-11-13T12:00:00Z"),ZoneOffset.UTC));
    }
    @AfterEach void close() { server.stop(0); }
    @Test void selectsLastBulletinAndStoresActualReferenceDate() {
        var quote = adapter.quote("USD");
        assertThat(quote.rate()).isEqualByComparingTo("5.2");
        assertThat(quote.date()).isEqualTo(LocalDate.parse("2026-11-12"));
        assertThat(quote.bulletin()).isEqualTo("FECHAMENTO PTAX");
        assertThat(path).isEqualTo("/api/cambio/v1/cotacao/USD/2026-11-12");
    }
    @Test void brlDoesNotCallExternalService() { assertThat(adapter.quote("BRL").rate()).isEqualByComparingTo("1"); assertThat(path).isNull(); }
    @Test void missingMalformedAndNonPositiveQuotesNeverProduceFallbackRate() {
        for (String invalid : new String[]{"{}", "not-json", QUOTES.replace("5.1","0"),QUOTES.replace("2026-11-12","2026-11-13"),QUOTES.replace("USD","EUR")}) {
            body=invalid; assertThatThrownBy(() -> adapter.quote("USD")).isInstanceOf(DependencyUnavailableException.class);
        }
    }
    @Test void outageIsTechnicalAndUnsupportedCurrencyIsBadInput() {
        status=503; assertThatThrownBy(() -> adapter.quote("USD")).isInstanceOf(DependencyUnavailableException.class);
        status=400; body="{\"name\":\"INVALID_CURRENCY_VALUE\"}";
        assertThatThrownBy(() -> adapter.quote("XXX")).isInstanceOf(InvalidOperationException.class);
    }
    @Test void effectiveDateMayBeEarlierThanRequestedButMustMatchBulletin() {
        body=QUOTES.replace("2026-11-12","2026-11-11");
        assertThat(adapter.quote("USD").date()).isEqualTo(LocalDate.parse("2026-11-11"));
    }
}
