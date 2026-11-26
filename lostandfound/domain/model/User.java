package com.campus.lostfound.domain.model;

import com.campus.lostfound.domain.enums.Role;

import java.time.Instant;

public final class User {
    private final long id;
    private final String email;
    private final String passwordHash;
    private final Role role;
    private final boolean active;
    private final Instant createdAt;

    public User(long id, String email, String passwordHash, Role role, boolean active, Instant createdAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = active;
        this.createdAt = createdAt;
    }

    public long id() { return id; }
    public String email() { return email; }
    public String passwordHash() { return passwordHash; }
    public Role role() { return role; }
    public boolean active() { return active; }
    public Instant createdAt() { return createdAt; }
}
