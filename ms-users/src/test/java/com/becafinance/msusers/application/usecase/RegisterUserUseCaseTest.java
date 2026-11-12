package com.becafinance.msusers.application.usecase;

import com.becafinance.msusers.application.port.out.*;
import com.becafinance.msusers.domain.exception.DuplicateEmailException;
import com.becafinance.msusers.domain.model.User;
import jakarta.validation.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterUserUseCaseTest {
    @Mock UserRepositoryPort repository;
    @Mock PasswordHasherPort passwords;
    static ValidatorFactory factory;
    CreateUserUseCase create;
    @BeforeAll static void openValidator() { factory = Validation.buildDefaultValidatorFactory(); }
    @AfterAll static void closeValidator() { factory.close(); }
    @BeforeEach void setup() { create = new CreateUserUseCase(repository, new UserInputValidator(factory.getValidator()), passwords); }

    @Test void persistsOnlyEncodedUnmodifiedPassword() {
        when(repository.findByEmail("ana@example.com")).thenReturn(Optional.empty());
        when(passwords.hash(" Secret123 ")).thenReturn("bcrypt-hash");
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        var user = create.execute("Ana", " ANA@EXAMPLE.COM ", " Secret123 ");
        assertThat(user.getPasswordHash()).isEqualTo("bcrypt-hash");
        assertThat(user.getEmail()).isEqualTo("ana@example.com");
        verify(passwords).hash(" Secret123 ");
    }

    @Test void rejectsMissingBlankShortAndOversizedPassword() {
        for (String password : new String[] {null, "", "        ", "short", "a".repeat(73), "é".repeat(37)}) {
            assertThatThrownBy(() -> create.execute("Ana", "ana@example.com", password))
                .isInstanceOf(ConstraintViolationException.class);
        }
        verifyNoInteractions(repository, passwords);
    }

    @Test void rejectsDuplicateBeforeHashing() {
        when(repository.findByEmail("ana@example.com")).thenReturn(Optional.of(new User(1L, "Ana", "ana@example.com", null)));
        assertThatThrownBy(() -> create.execute("Ana", "ana@example.com", "Secret123"))
            .isInstanceOf(DuplicateEmailException.class);
        verifyNoInteractions(passwords);
        verify(repository, never()).save(any());
    }

    @Test void csvCreationDoesNotGenerateCredentials() {
        when(repository.findByEmail("ana@example.com")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        assertThat(create.execute("Ana", "ana@example.com").getPasswordHash()).isNull();
        verifyNoInteractions(passwords);
    }
}
