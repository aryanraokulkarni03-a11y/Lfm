package com.campus.lostfound.service;

import com.campus.lostfound.domain.model.User;
import com.campus.lostfound.repository.api.UserRepository;
import java.util.List;

public final class AdminService {
    private final UserRepository users;
    public AdminService(UserRepository users) { this.users = users; }
    public List<User> users() { return users.findAll(); }
    public void setActive(long id, boolean active) { users.setActive(id, active); }
}
