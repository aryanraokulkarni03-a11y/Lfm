package com.campus.lostfound.service;

import com.campus.lostfound.domain.model.UserProfile;
import com.campus.lostfound.repository.api.ProfileRepository;
import java.util.Optional;

public final class ProfileService {
    private final ProfileRepository profiles;
    public ProfileService(ProfileRepository profiles) { this.profiles = profiles; }
    public Optional<UserProfile> find(long userId) { return profiles.findByUserId(userId); }
    public void save(UserProfile profile) { if (profile.fullName().isBlank() || profile.degree().isBlank() || profile.branch().isBlank() || profile.registrationNumber().isBlank() || profile.semester().isBlank() || profile.batch().isBlank() || profile.section().isBlank() || profile.hostelName().isBlank() || profile.roomNumber().isBlank() || profile.gender().isBlank()) throw new IllegalArgumentException("Complete all profile fields before saving"); if (!profile.phone().matches("\\d{10}")) throw new IllegalArgumentException("Phone number must contain exactly 10 digits"); profiles.save(profile); }
}
