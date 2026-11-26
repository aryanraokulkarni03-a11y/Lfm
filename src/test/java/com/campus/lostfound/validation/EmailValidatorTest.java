package com.campus.lostfound.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailValidatorTest {
    private final EmailValidator validator = new EmailValidator();

    @Test
    void acceptsOnlyTheUniversityDomain() {
        assertTrue(validator.isAllowed("student@srmap.edu.in"));
        assertTrue(validator.isAllowed("STUDENT@SRMAP.EDU.IN"));
        assertFalse(validator.isAllowed("student@gmail.com"));
        assertFalse(validator.isAllowed("student@srmap.edu.in.evil.com"));
    }
}
