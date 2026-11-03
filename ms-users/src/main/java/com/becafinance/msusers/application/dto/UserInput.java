package com.becafinance.msusers.application.dto;
import jakarta.validation.constraints.*;
public record UserInput(
    @NotBlank(message="O nome e obrigatorio") @Size(max=150) String name,
    @NotBlank(message="O e-mail e obrigatorio") @Email(message="E-mail invalido") @Size(max=255) String email) {}
