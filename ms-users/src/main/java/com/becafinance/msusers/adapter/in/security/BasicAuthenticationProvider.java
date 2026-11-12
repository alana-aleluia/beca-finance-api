package com.becafinance.msusers.adapter.in.security;

import com.becafinance.msusers.application.exception.InvalidCredentialsException;
import com.becafinance.msusers.application.port.in.AuthenticateUserPort;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.*;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class BasicAuthenticationProvider implements AuthenticationProvider {
    private final AuthenticateUserPort authenticate;
    private final Set<Long> admins;

    public BasicAuthenticationProvider(AuthenticateUserPort authenticate,
            @Value("${security.admin-user-ids:}") String adminIds) {
        this.authenticate = authenticate;
        this.admins = Arrays.stream(adminIds.split(",")).map(String::trim).filter(s -> !s.isEmpty())
            .map(Long::valueOf).collect(Collectors.toUnmodifiableSet());
    }

    @Override public Authentication authenticate(Authentication request) {
        try {
            var identity = authenticate.execute(request.getName(), (String) request.getCredentials());
            var authorities = admins.contains(identity.id())
                ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN")) : List.<SimpleGrantedAuthority>of();
            return UsernamePasswordAuthenticationToken.authenticated(identity, null, authorities);
        } catch (InvalidCredentialsException exception) {
            throw new BadCredentialsException("Credenciais invalidas");
        }
    }

    @Override public boolean supports(Class<?> type) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(type);
    }
}
