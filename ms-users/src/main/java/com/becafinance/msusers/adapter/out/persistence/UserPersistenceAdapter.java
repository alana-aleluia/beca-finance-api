package com.becafinance.msusers.adapter.out.persistence;
import com.becafinance.msusers.domain.model.User;
import com.becafinance.msusers.domain.port.out.UserRepositoryPort;
import com.becafinance.msusers.domain.exception.DuplicateEmailException;
import com.becafinance.msusers.adapter.out.persistence.entity.UserJpaEntity;
import com.becafinance.msusers.adapter.out.persistence.repository.UserJpaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public class UserPersistenceAdapter implements UserRepositoryPort {
    private final UserJpaRepository repository;
    public UserPersistenceAdapter(UserJpaRepository repository) { this.repository = repository; }
    @Override
    public User save(User user) {
        try {
            return toDomain(repository.saveAndFlush(new UserJpaEntity(
                user.getId(), user.getName(), user.getEmail(), user.getCreatedAt())));
        } catch (DataIntegrityViolationException exception) {
            if (repository.findByEmail(user.getEmail())
                .filter(found -> !found.getId().equals(user.getId())).isPresent()) {
                throw new DuplicateEmailException();
            }
            throw exception;
        }
    }
    @Override public List<User> findAll() {
        return repository.findAll(Sort.by("id")).stream().map(this::toDomain).toList();
    }
    @Override public Optional<User> findById(Long id) { return repository.findById(id).map(this::toDomain); }
    @Override public Optional<User> findByEmail(String email) { return repository.findByEmail(email).map(this::toDomain); }
    @Override public void deleteById(Long id) { repository.deleteById(id); }
    private User toDomain(UserJpaEntity entity) {
        return new User(entity.getId(), entity.getName(), entity.getEmail(), entity.getCreatedAt());
    }
}
