package com.campus.lostfound.repository.api;

import com.campus.lostfound.domain.model.User;

import java.util.Optional;
import java.util.List;

public interface UserRepository {
    Optional<User> findByEmail(String email);
    User save(String email, String passwordHash);
    List<User> findAll();
    void setActive(long id, boolean active);
}
