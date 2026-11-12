package com.becafinance.msusers.application.usecase;
import com.becafinance.msusers.application.dto.UserInput;
import com.becafinance.msusers.application.dto.UserRegistration;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;
@Component
public class UserInputValidator {
    private final Validator validator;
    public UserInputValidator(Validator validator) { this.validator = validator; }
    public void validate(String name, String email) {
        var violations = validator.validate(new UserInput(name, email));
        if (!violations.isEmpty()) throw new ConstraintViolationException(violations);
    }

    public void validateRegistration(UserRegistration input) {
        var violations = validator.validate(input);
        if (!violations.isEmpty()) throw new ConstraintViolationException(violations);
    }
}
