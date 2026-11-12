package com.becafinance.msusers.application.dto;
import jakarta.validation.constraints.*;
import java.util.Locale;
public record UserInput(
    @NotBlank(message="O nome é obrigatório") @Size(max=150) String name,
    @NotBlank(message="O e-mail é obrigatório") @Email(message="E-mail inválido") @Size(max=255) String email) {
    public UserInput {
        if (email != null) email = email.trim().toLowerCase(Locale.ROOT);
    }
}
