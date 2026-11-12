package com.becafinance.msusers.application.dto;
import java.util.List;
public record CsvRow(long record, List<String> values) {}
