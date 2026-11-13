package com.becafinance.mstransactions.adapter.in.security;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.*;

@Configuration @EnableWebSecurity
public class SecurityConfig {
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http, RemoteAuthenticationProvider provider) throws Exception {
        AuthenticationEntryPoint entry = (request, response, exception) -> {
            int status = exception instanceof AuthenticationServiceException ? 503 : 401;
            response.setStatus(status);
            if (status == 401) response.setHeader("WWW-Authenticate", "Basic realm=\"ms-transactions\", charset=\"UTF-8\"");
            response.setContentType("application/problem+json");
            response.getWriter().write("{\"status\":" + status + ",\"detail\":\"" +
                (status == 401 ? "Credenciais invalidas ou ausentes" : "MS1 indisponivel") + "\"}");
        };
        return http
            // Clientes Postman/serviços enviam Basic explicitamente; não há login por cookies.
            .csrf(csrf -> csrf.disable()).requestCache(cache -> cache.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(provider).httpBasic(basic -> basic.authenticationEntryPoint(entry))
            .exceptionHandling(errors -> errors.authenticationEntryPoint(entry).accessDeniedHandler((request, response, exception) -> {
                response.setStatus(403); response.setContentType("application/problem+json");
                response.getWriter().write("{\"status\":403,\"detail\":\"Acesso negado\"}");
            }))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers(HttpMethod.POST, "/transactions").authenticated()
                .requestMatchers(HttpMethod.GET, "/transactions", "/account/balance").authenticated()
                .requestMatchers(HttpMethod.GET, "/transactions/{id}").authenticated()
                .requestMatchers(HttpMethod.PUT, "/transactions/{id}").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/transactions/{id}").authenticated()
                .anyRequest().denyAll()).build();
    }
}
