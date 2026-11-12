package com.becafinance.msusers.application.usecase;

import com.becafinance.msusers.application.port.out.UserRepositoryPort;
import com.becafinance.msusers.domain.exception.DuplicateEmailException;
import com.becafinance.msusers.domain.model.User;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateUserUseCaseTest {
    @Mock UserRepositoryPort repository;
    static ValidatorFactory factory;
    CreateUserUseCase useCase;

    @BeforeAll static void openValidator() { factory = Validation.buildDefaultValidatorFactory(); }
    @AfterAll static void closeValidator() { factory.close(); }
    @BeforeEach void setup() { useCase = new CreateUserUseCase(repository, new UserInputValidator(factory.getValidator()), mock(com.becafinance.msusers.application.port.out.PasswordHasherPort.class)); }

    @Test void normalizesBeforeLookupAndPersistence() {
        when(repository.findByEmail("ana@example.com")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = useCase.execute("Ana", "  ANA@EXAMPLE.COM  ");

        assertThat(result.getName()).isEqualTo("Ana");
        assertThat(result.getEmail()).isEqualTo("ana@example.com");
        assertThat(result.getCreatedAt()).isNotNull();
        var order = inOrder(repository);
        order.verify(repository).findByEmail("ana@example.com");
        order.verify(repository).save(result);
    }

    @Test void rejectsNormalizedDuplicate() {
        when(repository.findByEmail("ana@example.com")).thenReturn(Optional.of(new User(1L, "Ana", "ana@example.com", null)));
        assertThatThrownBy(() -> useCase.execute("Ana", " ANA@example.com ")).isInstanceOf(DuplicateEmailException.class);
        verify(repository, never()).save(any());
    }

    @Test void rejectsInvalidEmailWithoutAccessingRepository() {
        assertThatThrownBy(() -> useCase.execute("Ana", "invalid")).isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(repository);
    }

    @Test void rejectsBlankNameWithoutAccessingRepository() {
        assertThatThrownBy(() -> useCase.execute(" ", "ana@example.com")).isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(repository);
    }

    @Test void rejectsNullEmailWithoutAccessingRepository() {
        assertThatThrownBy(() -> useCase.execute("Ana", null)).isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(repository);
    }
}
