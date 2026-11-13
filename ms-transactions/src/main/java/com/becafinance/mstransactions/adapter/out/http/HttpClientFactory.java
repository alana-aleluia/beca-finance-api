package com.becafinance.mstransactions.adapter.out.http;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

public final class HttpClientFactory {
    private HttpClientFactory() {}
    public static RestClient create(String url, int timeoutSeconds) {
        var uri = URI.create(url);
        if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) || uri.getHost() == null
                || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null)
            throw new IllegalArgumentException("Configure uma URL HTTP válida, sem credenciais, query ou fragmento");
        var timeout = Duration.ofSeconds(timeoutSeconds);
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(timeout)
            .followRedirects(HttpClient.Redirect.NEVER).build());
        factory.setReadTimeout(timeout);
        return RestClient.builder().baseUrl(url).requestFactory(factory).build();
    }
}
