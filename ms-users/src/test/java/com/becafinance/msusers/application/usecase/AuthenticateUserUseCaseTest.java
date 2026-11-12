package com.becafinance.msusers.application.usecase;

import com.becafinance.msusers.application.exception.InvalidCredentialsException;
import com.becafinance.msusers.application.port.out.*;
import com.becafinance.msusers.domain.model.User;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticateUserUseCaseTest {
    @Mock UserRepositoryPort repository;
    @Mock PasswordHasherPort passwords;
    AuthenticateUserUseCase authenticate;

    @BeforeEach void setup() {
        when(passwords.hash(anyString())).thenReturn("dummy-hash");
        authenticate = new AuthenticateUserUseCase(repository, passwords);
    }

    @Test void authenticatesNormalizedEmailWithoutChangingPassword() {
        when(repository.findByEmail("ana@example.com")).thenReturn(Optional.of(new User(1L, "Ana", "ana@example.com", null, "hash")));
        when(passwords.matches(" Secret123 ", "hash")).thenReturn(true);
        var identity = authenticate.execute(" ANA@EXAMPLE.COM ", " Secret123 ");
        assertThat(identity.id()).isEqualTo(1L);
        assertThat(identity.email()).isEqualTo("ana@example.com");
        verify(passwords).matches(" Secret123 ", "hash");
    }

    @Test void rejectsWrongPassword() {
        when(repository.findByEmail("ana@example.com")).thenReturn(Optional.of(new User(1L, "Ana", "ana@example.com", null, "hash")));
        assertThatThrownBy(() -> authenticate.execute("ana@example.com", "wrong"))
            .isInstanceOf(InvalidCredentialsException.class).hasMessage("Credenciais invalidas");
    }

    @Test void rejectsUnknownUserAfterDummyComparison() {
        when(repository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authenticate.execute("missing@example.com", "Secret123"))
            .isInstanceOf(InvalidCredentialsException.class);
        verify(passwords).matches("Secret123", "dummy-hash");
    }

    @Test void rejectsAccountWithoutPasswordEvenIfDummyComparisonMatches() {
        when(repository.findByEmail("ana@example.com")).thenReturn(Optional.of(new User(1L, "Ana", "ana@example.com", null)));
        when(passwords.matches("Secret123", "dummy-hash")).thenReturn(true);
        assertThatThrownBy(() -> authenticate.execute("ana@example.com", "Secret123"))
            .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test void rejectsNullOrOversizedCredentials() {
        assertThatThrownBy(() -> authenticate.execute(null, "secret")).isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> authenticate.execute("ana@example.com", null)).isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> authenticate.execute("ana@example.com", "é".repeat(37))).isInstanceOf(InvalidCredentialsException.class);
        verifyNoInteractions(repository);
    }
}
