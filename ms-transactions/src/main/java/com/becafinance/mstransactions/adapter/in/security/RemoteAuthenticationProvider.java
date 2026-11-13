package com.becafinance.mstransactions.adapter.in.security;
import com.becafinance.mstransactions.application.exception.*;
import com.becafinance.mstransactions.application.port.out.UserAuthenticationPort;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class RemoteAuthenticationProvider implements AuthenticationProvider {
    private final UserAuthenticationPort users;
    public RemoteAuthenticationProvider(UserAuthenticationPort users) { this.users = users; }
    @Override public Authentication authenticate(Authentication request) {
        try {
            var identity = users.authenticate(request.getName(), (String) request.getCredentials());
            return UsernamePasswordAuthenticationToken.authenticated(identity, null, List.of());
        } catch (InvalidCredentialsException exception) { throw new BadCredentialsException("Credenciais inválidas"); }
        catch (DependencyUnavailableException exception) { throw new AuthenticationServiceException("MS1 indisponível"); }
    }
    @Override public boolean supports(Class<?> type) { return UsernamePasswordAuthenticationToken.class.isAssignableFrom(type); }
}
