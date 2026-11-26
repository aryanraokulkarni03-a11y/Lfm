package com.campus.lostfound.repository.api;

import com.campus.lostfound.domain.model.UserProfile;
import java.util.Optional;

public interface ProfileRepository {
    Optional<UserProfile> findByUserId(long userId);
    void save(UserProfile profile);
}
