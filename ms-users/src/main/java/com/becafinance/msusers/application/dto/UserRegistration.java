package com.becafinance.msusers.application.dto;

import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;

public record UserRegistration(
    @NotBlank @Size(max = 150) String name,
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Size(min = 8, max = 72) String password) {
    public UserRegistration {
        email = new UserInput(name, email).email();
    }

    @AssertTrue(message = "A senha deve ter no maximo 72 bytes em UTF-8")
    @io.swagger.v3.oas.annotations.media.Schema(hidden = true)
    public boolean isPasswordWithinBcryptLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    @Override public String toString() { return "UserRegistration[redacted]"; }
}
