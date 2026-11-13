package com.becafinance.mstransactions.adapter.out.http;
import com.becafinance.mstransactions.adapter.out.mockapi.MockApiAccountAdapter;
import com.becafinance.mstransactions.adapter.out.ms1.Ms1AuthenticationAdapter;
import com.becafinance.mstransactions.application.exception.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

class ExternalAdaptersTest {
    HttpServer server;
    java.util.concurrent.ExecutorService executor;
    volatile String response;
    volatile int status;
    volatile String authorization;
    volatile String path;
    volatile int delay;
    String baseUrl;
    static final String ACCOUNT = "{\"id\":\"1\",\"userId\":2,\"initialBalance\":\"1000.00\",\"currency\":\"BRL\",\"purchaseLimit\":\"500.00\"}";
    @BeforeEach void start() throws Exception {
        status = 200; delay = 0;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        executor = Executors.newCachedThreadPool(); server.setExecutor(executor);
        server.createContext("/", exchange -> {
            authorization = exchange.getRequestHeaders().getFirst("Authorization"); path = exchange.getRequestURI().toString();
            try { if (delay > 0) Thread.sleep(delay); }
            catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
            var bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            try { exchange.sendResponseHeaders(status, bytes.length); exchange.getResponseBody().write(bytes); }
            finally { exchange.close(); }
        });
        server.start(); baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }
    @AfterEach void stop() { server.stop(0); executor.shutdownNow(); }
    @Test void readsRealContractShapeWithoutSendingUserCredentials() {
        response = "[" + ACCOUNT + "]";
        var result = new MockApiAccountAdapter(baseUrl + "/accounts", 2).load(2L);
        assertThat(result.initialBalance()).isEqualByComparingTo("1000.00");
        assertThat(result.purchaseLimit()).isEqualByComparingTo("500.00");
        assertThat(path).isEqualTo("/accounts?userId=2");
        assertThat(authorization).isNull();
    }
    @Test void rejectsDuplicateMissingAndMalformedAccounts() {
        var adapter = new MockApiAccountAdapter(baseUrl + "/accounts", 2);
        for (String body : new String[] {"[]", "[" + ACCOUNT + "," + ACCOUNT + "]", "not-json", "null",
                "[" + ACCOUNT.replace("1000.00", "-1.00") + "]", "[" + ACCOUNT.replace("BRL", "USD") + "]",
                "[" + ACCOUNT.replace("500.00", "0.001") + "]"}) {
            response = body;
            assertThatThrownBy(() -> adapter.load(2L)).isInstanceOf(DependencyUnavailableException.class);
        }
    }
    @Test void validatesOwnerEvenIfRemoteFilterReturnsOtherUsers() {
        response = "[" + ACCOUNT.replace("\"userId\":2", "\"userId\":12") + "]";
        assertThatThrownBy(() -> new MockApiAccountAdapter(baseUrl + "/accounts", 2).load(2L)).isInstanceOf(DependencyUnavailableException.class);
    }
    @Test void distinguishesInvalidCredentialsFromMs1TechnicalFailure() {
        var adapter = new Ms1AuthenticationAdapter(baseUrl, 2);
        response = "{}"; status = 401;
        assertThatThrownBy(() -> adapter.authenticate("ana@example.com", "test-password")).isInstanceOf(InvalidCredentialsException.class);
        status = 503;
        assertThatThrownBy(() -> adapter.authenticate("ana@example.com", "test-password")).isInstanceOf(DependencyUnavailableException.class);
        status = 200;
        assertThatThrownBy(() -> adapter.authenticate("ana@example.com", "test-password")).isInstanceOf(DependencyUnavailableException.class);
    }
    @Test void authenticatesOnlyAgainstAuthMe() {
        response = "{\"id\":2,\"email\":\"ana@example.com\"}";
        var user = new Ms1AuthenticationAdapter(baseUrl, 2).authenticate("ana@example.com", "test-password");
        assertThat(user.id()).isEqualTo(2L);
        assertThat(path).isEqualTo("/auth/me");
        assertThat(authorization).isEqualTo("Basic " + java.util.Base64.getEncoder().encodeToString("ana@example.com:test-password".getBytes(StandardCharsets.UTF_8)));
    }
    @Test void refusesRedirectsAndDoesNotForwardCredentialsElsewhere() {
        response = "{}"; status = 302;
        assertThatThrownBy(() -> new Ms1AuthenticationAdapter(baseUrl, 2).authenticate("ana@example.com", "test-password"))
            .isInstanceOf(DependencyUnavailableException.class);
    }
    @Test void timeoutIsTechnicalFailure() {
        response = "[" + ACCOUNT + "]"; delay = 1500;
        assertThatThrownBy(() -> new MockApiAccountAdapter(baseUrl + "/accounts", 1).load(2L))
            .isInstanceOf(DependencyUnavailableException.class);
    }
}
