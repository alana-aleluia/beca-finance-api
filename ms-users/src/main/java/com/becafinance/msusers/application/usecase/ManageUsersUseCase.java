package com.becafinance.msusers.application.usecase;
import com.becafinance.msusers.domain.model.User;
import com.becafinance.msusers.domain.port.out.UserRepositoryPort;
import com.becafinance.msusers.domain.exception.*;
import org.springframework.stereotype.Service;
import java.util.List;
@Service
public class ManageUsersUseCase {
    private final UserRepositoryPort repository;
    private final UserInputValidator validator;
    public ManageUsersUseCase(UserRepositoryPort repository, UserInputValidator validator) {
        this.repository = repository;
        this.validator = validator;
    }
    public List<User> list() { return repository.findAll(); }
    public User find(Long id) {
        return repository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }
    public User update(Long id, String name, String email) {
        var existing = find(id);
        validator.validate(name, email);
        repository.findByEmail(email).filter(user -> !user.getId().equals(id))
            .ifPresent(user -> { throw new DuplicateEmailException(); });
        return repository.save(new User(id, name, email, existing.getCreatedAt()));
    }
    public void delete(Long id) { find(id); repository.deleteById(id); }
}
