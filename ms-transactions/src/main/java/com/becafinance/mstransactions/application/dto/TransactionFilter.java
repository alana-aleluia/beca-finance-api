package com.becafinance.mstransactions.application.dto;
import java.time.*;
import jakarta.validation.constraints.*;
public record TransactionFilter(LocalDate from, LocalDate to,
    @Pattern(regexp = "PURCHASE|DEPOSIT|WITHDRAWAL|TRANSFER") String type,
    @Pattern(regexp = "APPROVED|REJECTED") String status,
    @Size(max = 100) String category,
    @Min(0) Integer page, @Min(1) @Max(200) Integer size) {
    @AssertTrue(message = "Período inválido")
    @io.swagger.v3.oas.annotations.media.Schema(hidden = true)
    public boolean isPeriodValid() { return from == null || to == null || !from.isAfter(to); }
    public int pageNumber() { return page == null ? 0 : page; }
    public int pageSize() { return size == null ? 50 : size; }
}
