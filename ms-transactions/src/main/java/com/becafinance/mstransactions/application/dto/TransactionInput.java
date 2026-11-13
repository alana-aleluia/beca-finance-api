package com.becafinance.mstransactions.application.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record TransactionInput(
    @NotNull @Pattern(regexp = "PURCHASE|DEPOSIT|WITHDRAWAL|TRANSFER") String type,
    @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
    @NotNull @Pattern(regexp = "[A-Z]{3}") String currency,
    @NotBlank @Size(max = 100) String category,
    @Positive Long destinationUserId,
    @Size(max = 500) String description) {
    public TransactionInput { if (category != null) category = category.trim(); }
    public TransactionInput(String type, java.math.BigDecimal amount, String currency, String category) {
        this(type, amount, currency, category, null, null);
    }
    @AssertTrue(message = "Informe destino somente para TRANSFER")
    @io.swagger.v3.oas.annotations.media.Schema(hidden = true)
    public boolean isDestinationValid() { return "TRANSFER".equals(type) ? destinationUserId != null : destinationUserId == null; }
}
