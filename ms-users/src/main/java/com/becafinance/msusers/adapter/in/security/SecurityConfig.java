package com.becafinance.msusers.adapter.in.security;

import com.becafinance.msusers.application.dto.UserIdentity;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, BasicAuthenticationProvider provider) throws Exception {
        return http
            // API para clientes Postman/serviços que enviam Authorization explicitamente, sem login por cookies.
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .requestCache(cache -> cache.disable())
            .authenticationProvider(provider)
            .httpBasic(withDefaults())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/users").permitAll()
                .requestMatchers(HttpMethod.GET, "/auth/me").authenticated()
                .requestMatchers(HttpMethod.GET, "/users").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/users/import").hasRole("ADMIN")
                .requestMatchers("/users/{id}").access((authentication, context) -> {
                    var principal = authentication.get().getPrincipal();
                    return new AuthorizationDecision(principal instanceof UserIdentity identity
                        && identity.id().toString().equals(context.getVariables().get("id")));
                })
                .anyRequest().denyAll())
            .build();
    }
}
