package com.campus.lostfound.validation;

public final class EmailValidator {
    private static final String UNIVERSITY_DOMAIN = "@srmap.edu.in";

    public boolean isAllowed(String email) {
        return email != null && email.trim().toLowerCase().endsWith(UNIVERSITY_DOMAIN);
    }
}
