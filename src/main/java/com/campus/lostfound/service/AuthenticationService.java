package com.campus.lostfound.service;

import com.campus.lostfound.domain.model.User;
import com.campus.lostfound.exception.AuthenticationException;
import com.campus.lostfound.exception.ValidationException;
import com.campus.lostfound.repository.api.UserRepository;
import com.campus.lostfound.security.PasswordHasher;
import com.campus.lostfound.validation.EmailValidator;

public final class AuthenticationService {
    private final UserRepository users;
    private final PasswordHasher hasher;
    private final EmailValidator emailValidator;

    public AuthenticationService(UserRepository users, PasswordHasher hasher, EmailValidator emailValidator) {
        this.users = users;
        this.hasher = hasher;
        this.emailValidator = emailValidator;
    }

    public User register(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        if (!emailValidator.isAllowed(normalizedEmail)) {
            throw new ValidationException("Registration requires an @srmap.edu.in email address");
        }
        if (password == null || password.length() < 8) {
            throw new ValidationException("Password must contain at least 8 characters");
        }
        if (users.findByEmail(normalizedEmail).isPresent()) {
            throw new ValidationException("An account already exists for this email");
        }
        return users.save(normalizedEmail, hasher.hash(password));
    }

    public User login(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        User user = users.findByEmail(normalizedEmail)
                .orElseThrow(() -> new AuthenticationException("Invalid email or password"));
        if (!user.active() || !hasher.matches(password, user.passwordHash())) {
            throw new AuthenticationException("Invalid email or password");
        }
        return user;
    }

    /** Restores an explicitly remembered device session without storing a password. */
    public User restore(String email) {
        String normalizedEmail = normalizeEmail(email);
        User user = users.findByEmail(normalizedEmail)
                .orElseThrow(() -> new AuthenticationException("Remembered account was not found"));
        if (!user.active()) throw new AuthenticationException("Remembered account is inactive");
        return user;
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) throw new ValidationException("Email is required");
        return email.trim().toLowerCase();
    }
}
