package com.becafinance.msusers.application.usecase;

import com.becafinance.msusers.application.dto.*;
import com.becafinance.msusers.application.exception.InvalidCsvException;
import com.becafinance.msusers.application.port.in.CreateUserPort;
import com.becafinance.msusers.application.port.out.*;
import com.becafinance.msusers.adapter.out.csv.CommonsUserCsvReader;
import com.becafinance.msusers.domain.exception.DuplicateEmailException;
import jakarta.validation.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImportUsersUseCaseTest {
    @Mock UserCsvReaderPort reader;
    @Mock CreateUserPort createUser;
    final InputStream input = InputStream.nullInputStream();

    @Test void importsAllValidRows() {
        when(reader.read(input)).thenReturn(List.of(row(2, "Ana", "ana@example.com"), row(3, "Bia", "bia@example.com")));
        var result = new ImportUsersUseCase(reader, createUser).execute(input);
        assertThat(result.processed()).isEqualTo(2);
        assertThat(result.created()).isEqualTo(2);
        assertThat(result.rejected()).isZero();
        assertThat(result.errors()).isEmpty();
        verify(createUser).execute("Ana", "ana@example.com");
        verify(createUser).execute("Bia", "bia@example.com");
    }

    @Test void reportsRowErrorsAndContinuesImporting() {
        when(reader.read(input)).thenReturn(List.of(
            row(2, "only name"), row(3, "Ana", "duplicate@example.com"),
            row(4, "Ana", "bad"), row(5, "Bia", "bia@example.com")));
        when(createUser.execute("Ana", "duplicate@example.com")).thenThrow(new DuplicateEmailException());
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var violations = factory.getValidator().validate(new UserInput("Ana", "bad"));
            when(createUser.execute("Ana", "bad")).thenThrow(new ConstraintViolationException(violations));
            var result = new ImportUsersUseCase(reader, createUser).execute(input);
            assertThat(result.processed()).isEqualTo(4);
            assertThat(result.created()).isEqualTo(1);
            assertThat(result.rejected()).isEqualTo(3);
            assertThat(result.errors()).extracting(ImportResult.RowError::code)
                .containsExactly("INVALID_COLUMNS", "DUPLICATE_EMAIL", "VALIDATION_ERROR");
            assertThat(result.errors()).extracting(ImportResult.RowError::record).containsExactly(2L, 3L, 4L);
            assertThat(result.errors().get(2).messages()).isNotEmpty();
            verify(createUser).execute("Bia", "bia@example.com");
            verify(createUser, times(3)).execute(anyString(), anyString());
        }
    }

    @Test void malformedFileDoesNotCreateUsers() {
        when(reader.read(input)).thenThrow(new InvalidCsvException("bad file"));
        assertThatThrownBy(() -> new ImportUsersUseCase(reader, createUser).execute(input)).isInstanceOf(InvalidCsvException.class);
        verifyNoInteractions(createUser);
    }

    @Test void unexpectedPersistenceFailureIsNotReportedAsValidationError() {
        when(reader.read(input)).thenReturn(List.of(row(2, "Ana", "ana@example.com")));
        when(createUser.execute(anyString(), anyString())).thenThrow(new IllegalStateException("unavailable"));
        assertThatThrownBy(() -> new ImportUsersUseCase(reader, createUser).execute(input))
            .isInstanceOf(IllegalStateException.class).hasMessage("unavailable");
    }

    @Test void csvUsesTheSameNormalizationAndValidationAsCreation() {
        var repository = mock(UserRepositoryPort.class);
        when(repository.findByEmail("ana@example.com")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var creator = new CreateUserUseCase(repository, new UserInputValidator(factory.getValidator()), mock(com.becafinance.msusers.application.port.out.PasswordHasherPort.class));
            var importer = new ImportUsersUseCase(new CommonsUserCsvReader(), creator);
            var csv = new ByteArrayInputStream("name,email\nAna, ANA@EXAMPLE.COM \n".getBytes(StandardCharsets.UTF_8));
            assertThat(importer.execute(csv).created()).isEqualTo(1);
            verify(repository).save(argThat(user -> user.getEmail().equals("ana@example.com")));
        }
    }

    private CsvRow row(long number, String... values) { return new CsvRow(number, List.of(values)); }
}
