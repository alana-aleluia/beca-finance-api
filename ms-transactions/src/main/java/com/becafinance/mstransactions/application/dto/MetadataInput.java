package com.becafinance.mstransactions.application.dto;
import jakarta.validation.constraints.*;
public record MetadataInput(@NotBlank @Size(max = 100) String category, @Size(max = 500) String description) {
    public MetadataInput { if (category != null) category = category.trim(); }
}
