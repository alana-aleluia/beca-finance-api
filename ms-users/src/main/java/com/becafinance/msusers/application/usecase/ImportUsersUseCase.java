package com.becafinance.msusers.application.usecase;

import com.becafinance.msusers.application.dto.ImportResult;
import com.becafinance.msusers.application.dto.ImportResult.RowError;
import com.becafinance.msusers.application.port.out.UserCsvReader;
import com.becafinance.msusers.domain.exception.DuplicateEmailException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.stereotype.Service;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class ImportUsersUseCase {
    private final UserCsvReader csvReader;
    private final CreateUserUseCase createUser;
    public ImportUsersUseCase(UserCsvReader csvReader, CreateUserUseCase createUser) {
        this.csvReader = csvReader;
        this.createUser = createUser;
    }
    public ImportResult execute(InputStream input) {
        var rows = csvReader.read(input);
        var errors = new ArrayList<RowError>();
        int created = 0;
        for (var row : rows) {
            if (row.values().size() != 2) {
                errors.add(new RowError(row.record(), "INVALID_COLUMNS",
                    List.of("Registro deve conter exatamente duas colunas: name,email")));
                continue;
            }
            try {
                createUser.execute(row.values().get(0), row.values().get(1));
                created++;
            } catch (ConstraintViolationException exception) {
                errors.add(new RowError(row.record(), "VALIDATION_ERROR",
                    exception.getConstraintViolations().stream()
                        .map(v -> v.getPropertyPath() + ": " + v.getMessage()).sorted().toList()));
            } catch (DuplicateEmailException exception) {
                errors.add(new RowError(row.record(), "DUPLICATE_EMAIL", List.of(exception.getMessage())));
            }
        }
        return new ImportResult(rows.size(), created, errors.size(), List.copyOf(errors));
    }
}
