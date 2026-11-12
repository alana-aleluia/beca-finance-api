package com.becafinance.msusers.application.usecase;

import com.becafinance.msusers.application.dto.*;
import com.becafinance.msusers.application.exception.InvalidCredentialsException;
import com.becafinance.msusers.application.port.in.AuthenticateUserPort;
import com.becafinance.msusers.application.port.out.*;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;

@Service
public class AuthenticateUserUseCase implements AuthenticateUserPort {
    private final UserRepositoryPort repository;
    private final PasswordHasherPort passwords;
    private final String dummyHash;

    public AuthenticateUserUseCase(UserRepositoryPort repository, PasswordHasherPort passwords) {
        this.repository = repository;
        this.passwords = passwords;
        // Mantém a comparação BCrypt também para contas desconhecidas. Este hash nunca é uma credencial de usuário.
        this.dummyHash = passwords.hash(java.util.UUID.randomUUID().toString());
    }

    @Override public UserIdentity execute(String email, String password) {
        if (email == null || password == null || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new InvalidCredentialsException();
        var user = repository.findByEmail(new UserInput(null, email).email()).orElse(null);
        var hash = user == null || user.getPasswordHash() == null ? dummyHash : user.getPasswordHash();
        boolean valid = passwords.matches(password, hash);
        if (user == null || user.getPasswordHash() == null || !valid) throw new InvalidCredentialsException();
        return new UserIdentity(user.getId(), user.getEmail());
    }
}
