package com.becafinance.msusers.adapter.in.web;

import com.becafinance.msusers.adapter.in.security.*;
import com.becafinance.msusers.adapter.out.csv.CommonsUserCsvReader;
import com.becafinance.msusers.adapter.out.security.BcryptPasswordHasher;
import com.becafinance.msusers.application.port.out.UserRepositoryPort;
import com.becafinance.msusers.application.usecase.*;
import com.becafinance.msusers.domain.model.User;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.OffsetDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({UserController.class, UserImportController.class, AuthController.class})
@Import({SecurityConfig.class, BasicAuthenticationProvider.class, AuthenticateUserUseCase.class,
    CreateUserUseCase.class, ManageUsersUseCase.class, ImportUsersUseCase.class,
    UserInputValidator.class, CommonsUserCsvReader.class, BcryptPasswordHasher.class})
@TestPropertySource(properties = "security.admin-user-ids=99")
class HttpSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean UserRepositoryPort repository;
    static final BcryptPasswordHasher passwords = new BcryptPasswordHasher();
    static final String hash = passwords.hash(" Secret123 ");
    User ana;

    @BeforeEach void setup() {
        ana = new User(1L, "Ana", "ana@example.com", OffsetDateTime.parse("2026-01-01T00:00:00Z"), hash);
        var admin = new User(99L, "Admin", "admin@example.com", ana.getCreatedAt(), hash);
        var imported = new User(2L, "Imported", "imported@example.com", ana.getCreatedAt());
        when(repository.findByEmail(anyString())).thenAnswer(call -> {
            String email = call.getArgument(0);
            return List.of(ana, admin, imported).stream().filter(user -> user.getEmail().equals(email)).findFirst();
        });
        when(repository.findById(1L)).thenReturn(Optional.of(ana));
        when(repository.findAll()).thenReturn(List.of(ana, admin, imported));
        when(repository.save(any())).thenAnswer(call -> {
            User user = call.getArgument(0);
            return new User(user.getId() == null ? 10L : user.getId(), user.getName(), user.getEmail(), user.getCreatedAt(), user.getPasswordHash());
        });
    }

    @Test void registersWithPasswordWithoutExposingCredentials() throws Exception {
        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Bia","email":" BIA@example.com ","password":" Secret123 "}
                    """))
            .andExpect(status().isCreated()).andExpect(header().string("Location", "/users/10"))
            .andExpect(jsonPath("$.email").value("bia@example.com"))
            .andExpect(jsonPath("$.password").doesNotExist()).andExpect(jsonPath("$.passwordHash").doesNotExist())
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("$2"))));
        verify(repository).save(argThat(user -> passwords.matches(" Secret123 ", user.getPasswordHash())));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"name\":\"Ana\",\"email\":\"ana@example.com\"}",
        "{\"name\":\"Ana\",\"email\":\"ana@example.com\",\"password\":\"\"}",
        "{\"name\":\"Ana\",\"email\":\"ana@example.com\",\"password\":\"short\"}"})
    void rejectsInvalidRegistrationWithoutSaving(String body) throws Exception {
        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        verify(repository, never()).save(any());
    }

    @Test void validatesBasicAndReturnsOnlyIdentity() throws Exception {
        mvc.perform(get("/auth/me").with(httpBasic(" ANA@example.com ", " Secret123 ")))
            .andExpect(status().isOk()).andExpect(content().json("{\"id\":1,\"email\":\"ana@example.com\"}"))
            .andExpect(jsonPath("$.name").doesNotExist()).andExpect(jsonPath("$.passwordHash").doesNotExist())
            .andExpect(jsonPath("$.password").doesNotExist()).andExpect(header().doesNotExist("Set-Cookie"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ana@example.com", "missing@example.com", "imported@example.com"})
    void invalidCredentialsUnknownAndPasswordlessUsersReturn401(String email) throws Exception {
        mvc.perform(get("/auth/me").with(httpBasic(email, "wrong")))
            .andExpect(status().isUnauthorized()).andExpect(header().exists("WWW-Authenticate"));
    }

    @Test void passwordIsNotTrimmed() throws Exception {
        mvc.perform(get("/auth/me").with(httpBasic("ana@example.com", "Secret123"))).andExpect(status().isUnauthorized());
    }

    @Test void everyRequestRequiresCredentials() throws Exception {
        mvc.perform(get("/auth/me").with(httpBasic("ana@example.com", " Secret123 "))).andExpect(status().isOk());
        mvc.perform(get("/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/users/1")).andExpect(status().isUnauthorized());
    }

    @Test void userCanReadUpdateAndDeleteOnlyOwnProfile() throws Exception {
        mvc.perform(get("/users/1").with(httpBasic("ana@example.com", " Secret123 ")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(put("/users/1").with(httpBasic("ana@example.com", " Secret123 "))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Ana Silva\",\"email\":\" ANA@example.com \"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.passwordHash").doesNotExist());
        verify(repository).save(argThat(user -> hash.equals(user.getPasswordHash()) && user.getCreatedAt().equals(ana.getCreatedAt())));
        mvc.perform(delete("/users/1").with(httpBasic("ana@example.com", " Secret123 "))).andExpect(status().isNoContent());
        verify(repository).deleteById(1L);
    }

    @Test void deniesAccessToAnotherProfileEvenForAdmin() throws Exception {
        mvc.perform(get("/users/2").with(httpBasic("ana@example.com", " Secret123 "))).andExpect(status().isForbidden());
        mvc.perform(put("/users/2").with(httpBasic("ana@example.com", " Secret123 "))
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Other\",\"email\":\"other@example.com\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(delete("/users/2").with(httpBasic("ana@example.com", " Secret123 "))).andExpect(status().isForbidden());
        mvc.perform(get("/users/1").with(httpBasic("admin@example.com", " Secret123 "))).andExpect(status().isForbidden());
        verify(repository, never()).findById(2L);
        verify(repository, never()).save(any());
        verify(repository, never()).deleteById(any());
    }

    @Test void listAndImportRequireAdmin() throws Exception {
        mvc.perform(get("/users").with(httpBasic("ana@example.com", " Secret123 "))).andExpect(status().isForbidden());
        mvc.perform(multipart("/users/import").file(csv()).with(httpBasic("ana@example.com", " Secret123 "))).andExpect(status().isForbidden());
        mvc.perform(get("/users").with(httpBasic("admin@example.com", " Secret123 ")))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].passwordHash").doesNotExist());
        mvc.perform(multipart("/users/import").file(csv()).with(httpBasic("admin@example.com", " Secret123 ")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.created").value(1));
        verify(repository).save(argThat(user -> user.getEmail().equals("csv@example.com") && user.getPasswordHash() == null));
    }

    @Test void publicRegistrationCannotGrantAdminAuthority() throws Exception {
        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"New\",\"email\":\"new@example.com\",\"password\":\"Secret123\",\"role\":\"ADMIN\"}"))
            .andExpect(status().isCreated());
        verify(repository).save(argThat(user -> user.getEmail().equals("new@example.com")));
    }

    private MockMultipartFile csv() {
        return new MockMultipartFile("file", "users.csv", "text/csv", "name,email\nCSV,csv@example.com\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
