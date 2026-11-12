package com.becafinance.msusers.application.usecase;
import com.becafinance.msusers.domain.model.User;
import com.becafinance.msusers.application.port.out.UserRepositoryPort;
import com.becafinance.msusers.domain.exception.DuplicateEmailException;
import org.springframework.stereotype.Service;
import com.becafinance.msusers.application.port.in.CreateUserPort;
import com.becafinance.msusers.application.dto.UserInput;
import com.becafinance.msusers.application.dto.UserRegistration;
import com.becafinance.msusers.application.port.out.PasswordHasherPort;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
@Service
public class CreateUserUseCase implements CreateUserPort {
    private final UserRepositoryPort repository;
    private final UserInputValidator validator;
    private final PasswordHasherPort passwords;
    public CreateUserUseCase(UserRepositoryPort repository, UserInputValidator validator, PasswordHasherPort passwords) {
        this.repository = repository;
        this.validator = validator;
        this.passwords = passwords;
    }
    public User execute(String name, String email) {
        email = new UserInput(name, email).email();
        validator.validate(name, email);
        if (repository.findByEmail(email).isPresent()) throw new DuplicateEmailException();
        return repository.save(new User(null, name, email, OffsetDateTime.now(ZoneOffset.UTC)));
    }

    @Override
    public User execute(String name, String email, String password) {
        var input = new UserRegistration(name, email, password);
        validator.validateRegistration(input);
        if (repository.findByEmail(input.email()).isPresent()) throw new DuplicateEmailException();
        return repository.save(new User(null, input.name(), input.email(), OffsetDateTime.now(ZoneOffset.UTC), passwords.hash(password)));
    }
}
