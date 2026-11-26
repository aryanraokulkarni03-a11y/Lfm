package com.campus.lostfound.service;

import com.campus.lostfound.domain.model.User;
import com.campus.lostfound.exception.AuthenticationException;
import com.campus.lostfound.exception.ValidationException;
import com.campus.lostfound.repository.api.UserRepository;
import com.campus.lostfound.security.PasswordHasher;
import com.campus.lostfound.validation.EmailValidator;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticationServiceTest {
    private final FakeUsers users = new FakeUsers();
    private final AuthenticationService service = new AuthenticationService(users, new PasswordHasher(), new EmailValidator());

    @Test
    void registersAndLogsInAUniversityUser() {
        User created = service.register(" DEMO@SRMAP.EDU.IN ", "correct horse battery staple");

        assertEquals("demo@srmap.edu.in", created.email());
        assertNotEquals("correct horse battery staple", created.passwordHash());
        assertEquals(created.id(), service.login("demo@srmap.edu.in", "correct horse battery staple").id());
    }

    @Test
    void rejectsInvalidDomainAndWrongPassword() {
        assertThrows(ValidationException.class, () -> service.register("demo@gmail.com", "password123"));
        service.register("demo@srmap.edu.in", "password123");
        assertThrows(AuthenticationException.class, () -> service.login("demo@srmap.edu.in", "wrongpass"));
    }

    @Test
    void restoresOnlyAnActiveRememberedAccount() {
        User created = service.register("demo@srmap.edu.in", "password123");

        assertEquals(created.id(), service.restore("DEMO@SRMAP.EDU.IN").id());
    }

    private static final class FakeUsers implements UserRepository {
        private final Map<String, User> users = new HashMap<>();
        private long nextId = 1;

        @Override
        public Optional<User> findByEmail(String email) {
            return Optional.ofNullable(users.get(email));
        }

        @Override
        public User save(String email, String passwordHash) {
            User user = new User(nextId++, email, passwordHash,
                    com.campus.lostfound.domain.enums.Role.STUDENT, true, Instant.now());
            users.put(email, user);
            return user;
        }

        @Override public List<User> findAll() { return List.copyOf(users.values()); }
        @Override public void setActive(long id, boolean active) { }
    }
}
