package com.becafinance.mstransactions.adapter.out.ms1;
import com.becafinance.mstransactions.adapter.out.http.HttpClientFactory;
import com.becafinance.mstransactions.application.dto.UserIdentity;
import com.becafinance.mstransactions.application.exception.*;
import com.becafinance.mstransactions.application.port.out.UserAuthenticationPort;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class Ms1AuthenticationAdapter implements UserAuthenticationPort {
    private final RestClient client;
    public Ms1AuthenticationAdapter(@Value("${integrations.ms1-url}") String url,
            @Value("${integrations.timeout-seconds:5}") int timeout) {
        client = HttpClientFactory.create(url, timeout);
    }
    @Override public UserIdentity authenticate(String email, String password) {
        try {
            var response = client.get().uri("/auth/me")
                .headers(headers -> headers.setBasicAuth(email, password, StandardCharsets.UTF_8))
                .retrieve().onStatus(status -> status.value() == 401, (request, result) -> { throw new InvalidCredentialsException(); })
                .toEntity(IdentityResponse.class);
            var identity = response.getBody();
            if (!response.getStatusCode().is2xxSuccessful() || identity == null || identity.id() == null || identity.id() <= 0
                    || identity.email() == null || identity.email().isBlank())
                throw new DependencyUnavailableException("Resposta inválida do MS1");
            return new UserIdentity(identity.id(), identity.email());
        } catch (InvalidCredentialsException exception) { throw exception; }
        catch (RuntimeException exception) { throw new DependencyUnavailableException("Não foi possível validar a identidade no MS1"); }
    }
    public record IdentityResponse(Long id, String email) {}
}
