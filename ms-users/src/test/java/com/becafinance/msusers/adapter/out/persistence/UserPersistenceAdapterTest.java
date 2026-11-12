package com.becafinance.msusers.adapter.out.persistence;

import com.becafinance.msusers.adapter.out.persistence.entity.UserJpaEntity;
import com.becafinance.msusers.adapter.out.persistence.repository.UserJpaRepository;
import com.becafinance.msusers.domain.model.User;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.OffsetDateTime;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserPersistenceAdapterTest {
    @Mock UserJpaRepository repository;

    @Test void roundTripsPasswordHashThroughSaveMapping() {
        var adapter = new UserPersistenceAdapter(repository);
        var created = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        when(repository.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        var saved = adapter.save(new User(1L, "Ana", "ana@example.com", created, "stored-hash"));
        verify(repository).saveAndFlush(argThat(entity -> "stored-hash".equals(entity.getPasswordHash())));
        assertThat(saved.getPasswordHash()).isEqualTo("stored-hash");
        assertThat(saved.getCreatedAt()).isEqualTo(created);
    }

    @Test void loadsHashAndAllowsNullForImportedUsers() {
        var adapter = new UserPersistenceAdapter(repository);
        when(repository.findByEmail("ana@example.com")).thenReturn(Optional.of(
            new UserJpaEntity(1L, "Ana", "ana@example.com", null, "stored-hash")));
        when(repository.findById(2L)).thenReturn(Optional.of(
            new UserJpaEntity(2L, "CSV", "csv@example.com", null, null)));
        assertThat(adapter.findByEmail("ana@example.com").orElseThrow().getPasswordHash()).isEqualTo("stored-hash");
        assertThat(adapter.findById(2L).orElseThrow().getPasswordHash()).isNull();
    }
}
