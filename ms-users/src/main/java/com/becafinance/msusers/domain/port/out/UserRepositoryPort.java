package com.becafinance.msusers.domain.port.out;
import com.becafinance.msusers.domain.model.User;
import java.util.List;
import java.util.Optional;
public interface UserRepositoryPort {
    User save(User user);
    List<User> findAll();
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
    void deleteById(Long id);
}
