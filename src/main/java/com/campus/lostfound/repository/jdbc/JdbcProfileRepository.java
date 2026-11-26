package com.campus.lostfound.repository.jdbc;

import com.campus.lostfound.config.ConnectionProvider;
import com.campus.lostfound.domain.model.UserProfile;
import com.campus.lostfound.repository.api.ProfileRepository;
import java.sql.SQLException;
import java.util.Optional;

public final class JdbcProfileRepository implements ProfileRepository {
    private final ConnectionProvider connections;
    public JdbcProfileRepository(ConnectionProvider connections) { this.connections = connections; }
    @Override public Optional<UserProfile> findByUserId(long userId) {
        try (var connection = connections.open(); var statement = connection.prepareStatement("SELECT user_id,full_name,degree,branch,registration_number,semester,batch,section,hostel_name,room_number,phone,gender FROM user_profiles WHERE user_id=?")) { statement.setLong(1, userId); try (var rows = statement.executeQuery()) { return rows.next() ? Optional.of(new UserProfile(rows.getLong("user_id"), rows.getString("full_name"), rows.getString("degree"), rows.getString("branch"), rows.getString("registration_number"), rows.getString("semester"), rows.getString("batch"), rows.getString("section"), rows.getString("hostel_name"), rows.getString("room_number"), rows.getString("phone"), rows.getString("gender"))) : Optional.empty(); } }
        catch (SQLException ex) { throw new IllegalStateException("Could not load profile", ex); }
    }
    @Override public void save(UserProfile profile) {
        String sql = "INSERT INTO user_profiles (user_id,full_name,degree,branch,registration_number,semester,batch,section,hostel_name,room_number,phone,gender,university_details) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE full_name=VALUES(full_name), degree=VALUES(degree), branch=VALUES(branch), registration_number=VALUES(registration_number), semester=VALUES(semester), batch=VALUES(batch), section=VALUES(section), hostel_name=VALUES(hostel_name), room_number=VALUES(room_number), phone=VALUES(phone), gender=VALUES(gender), university_details=VALUES(university_details)";
        try (var connection = connections.open(); var statement = connection.prepareStatement(sql)) { statement.setLong(1, profile.userId()); statement.setString(2, profile.fullName().trim()); statement.setString(3, profile.degree().trim()); statement.setString(4, profile.branch().trim()); statement.setString(5, profile.registrationNumber().trim()); statement.setString(6, profile.semester().trim()); statement.setString(7, profile.batch().trim()); statement.setString(8, profile.section().trim()); statement.setString(9, profile.hostelName().trim()); statement.setString(10, profile.roomNumber().trim()); statement.setString(11, profile.phone().trim()); statement.setString(12, profile.gender().trim()); statement.setString(13, profile.universityDetails().trim()); statement.executeUpdate(); }
        catch (SQLException ex) { throw new IllegalStateException("Could not save profile", ex); }
    }
}
