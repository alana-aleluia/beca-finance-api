package com.becafinance.msusers.application.usecase;

import com.becafinance.msusers.application.port.out.UserRepositoryPort;
import com.becafinance.msusers.domain.exception.*;
import com.becafinance.msusers.domain.model.User;
import jakarta.validation.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.OffsetDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManageUsersUseCaseTest {
    @Mock UserRepositoryPort repository;
    static ValidatorFactory factory;
    ManageUsersUseCase useCase;
    final User existing = new User(1L, "Ana", "ana@example.com", OffsetDateTime.parse("2026-01-01T00:00:00Z"));

    @BeforeAll static void openValidator() { factory = Validation.buildDefaultValidatorFactory(); }
    @AfterAll static void closeValidator() { factory.close(); }
    @BeforeEach void setup() { useCase = new ManageUsersUseCase(repository, new UserInputValidator(factory.getValidator())); }

    @Test void listsUsers() {
        when(repository.findAll()).thenReturn(List.of(existing));
        assertThat(useCase.list()).containsExactly(existing);
    }
    @Test void findsUser() {
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        assertThat(useCase.find(1L)).isSameAs(existing);
    }
    @Test void reportsMissingUser() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.find(1L)).isInstanceOf(UserNotFoundException.class);
    }
    @Test void updatesOwnEmailPreservingIdentityAndCreationDate() {
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.findByEmail("ana@example.com")).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var updated = useCase.update(1L, "Ana Silva", " ANA@EXAMPLE.COM ");
        assertThat(updated.getId()).isEqualTo(1L);
        assertThat(updated.getName()).isEqualTo("Ana Silva");
        assertThat(updated.getEmail()).isEqualTo("ana@example.com");
        assertThat(updated.getCreatedAt()).isEqualTo(existing.getCreatedAt());
    }
    @Test void updatesToAnUnusedNormalizedEmail() {
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertThat(useCase.update(1L, "Ana", " NEW@example.com ").getEmail()).isEqualTo("new@example.com");
    }
    @Test void rejectsEmailOwnedByAnotherUser() {
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.findByEmail("other@example.com")).thenReturn(Optional.of(new User(2L, "Other", "other@example.com", null)));
        assertThatThrownBy(() -> useCase.update(1L, "Ana", " OTHER@example.com ")).isInstanceOf(DuplicateEmailException.class);
        verify(repository, never()).save(any());
    }
    @Test void rejectsInvalidUpdate() {
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        assertThatThrownBy(() -> useCase.update(1L, "Ana", "bad")).isInstanceOf(ConstraintViolationException.class);
        verify(repository, never()).findByEmail(any());
        verify(repository, never()).save(any());
    }
    @Test void doesNotSaveMissingUser() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.update(1L, "Ana", "ana@example.com")).isInstanceOf(UserNotFoundException.class);
        verify(repository, never()).save(any());
    }
    @Test void deletesExistingUser() {
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        useCase.delete(1L);
        verify(repository).deleteById(1L);
    }
    @Test void doesNotDeleteMissingUser() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.delete(1L)).isInstanceOf(UserNotFoundException.class);
        verify(repository, never()).deleteById(any());
    }
}
