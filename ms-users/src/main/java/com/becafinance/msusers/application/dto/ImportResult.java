package com.becafinance.msusers.application.dto;
import java.util.List;
public record ImportResult(int processed, int created, int rejected, List<RowError> errors) {
    public record RowError(long record, String code, List<String> messages) {}
}
