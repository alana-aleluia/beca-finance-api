package com.becafinance.msusers.adapter.in.web;
import com.becafinance.msusers.adapter.in.web.dto.UserResponse;
import com.becafinance.msusers.application.dto.UserInput;
import com.becafinance.msusers.application.usecase.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;
@RestController
@RequestMapping("/users")
public class UserController {
    private final CreateUserUseCase createUser;
    private final ManageUsersUseCase users;
    public UserController(CreateUserUseCase createUser, ManageUsersUseCase users) {
        this.createUser = createUser;
        this.users = users;
    }
    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserInput request) {
        var created = createUser.execute(request.name(), request.email());
        return ResponseEntity.created(URI.create("/users/" + created.getId())).body(UserResponse.from(created));
    }
    @GetMapping public List<UserResponse> list() { return users.list().stream().map(UserResponse::from).toList(); }
    @GetMapping("/{id}") public UserResponse find(@PathVariable Long id) { return UserResponse.from(users.find(id)); }
    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserInput request) {
        return UserResponse.from(users.update(id, request.name(), request.email()));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        users.delete(id);
        return ResponseEntity.noContent().build();
    }
}
