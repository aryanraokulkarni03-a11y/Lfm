package com.campus.lostfound.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {
    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void hashesAreSaltedAndPasswordsCanBeVerified() {
        String first = hasher.hash("correct horse battery staple");
        String second = hasher.hash("correct horse battery staple");

        assertNotEquals(first, second);
        assertTrue(hasher.matches("correct horse battery staple", first));
        assertFalse(hasher.matches("wrong password", first));
    }
}
