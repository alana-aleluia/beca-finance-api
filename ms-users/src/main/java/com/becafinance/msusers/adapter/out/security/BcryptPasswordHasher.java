package com.becafinance.msusers.adapter.out.security;

import com.becafinance.msusers.application.port.out.PasswordHasherPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BcryptPasswordHasher implements PasswordHasherPort {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    @Override public String hash(String password) { return encoder.encode(password); }
    @Override public boolean matches(String password, String hash) {
        return encoder.matches(password, hash);
    }
}
