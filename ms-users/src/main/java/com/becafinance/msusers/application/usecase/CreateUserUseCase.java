package com.becafinance.msusers.application.usecase;
import com.becafinance.msusers.domain.model.User;
import com.becafinance.msusers.domain.port.out.UserRepositoryPort;
import com.becafinance.msusers.domain.exception.DuplicateEmailException;
import org.springframework.stereotype.Service;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
@Service
public class CreateUserUseCase {
    private final UserRepositoryPort repository;
    private final UserInputValidator validator;
    public CreateUserUseCase(UserRepositoryPort repository, UserInputValidator validator) {
        this.repository = repository;
        this.validator = validator;
    }
    public User execute(String name, String email) {
        validator.validate(name, email);
        if (repository.findByEmail(email).isPresent()) throw new DuplicateEmailException();
        return repository.save(new User(null, name, email, OffsetDateTime.now(ZoneOffset.UTC)));
    }
}
