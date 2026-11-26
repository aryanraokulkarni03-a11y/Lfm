package com.campus.lostfound.repository.jdbc;

import com.campus.lostfound.config.ConnectionProvider;
import com.campus.lostfound.domain.enums.Role;
import com.campus.lostfound.domain.model.User;
import com.campus.lostfound.repository.api.UserRepository;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;

public final class JdbcUserRepository implements UserRepository {
    private final ConnectionProvider connections;

    public JdbcUserRepository(ConnectionProvider connections) {
        this.connections = connections;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT id, email, password_hash, role, active, created_at FROM users WHERE email = ?";
        try (var connection = connections.open(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (var result = statement.executeQuery()) {
                if (!result.next()) return Optional.empty();
                return Optional.of(new User(
                        result.getLong("id"),
                        result.getString("email"),
                        result.getString("password_hash"),
                        Role.valueOf(result.getString("role")),
                        result.getBoolean("active"),
                        result.getTimestamp("created_at").toInstant()
                ));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Could not find user", ex);
        }
    }

    @Override
    public User save(String email, String passwordHash) {
        String sql = "INSERT INTO users (email, password_hash) VALUES (?, ?)";
        try (var connection = connections.open(); var statement = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, email);
            statement.setString(2, passwordHash);
            statement.executeUpdate();
            try (var keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new IllegalStateException("User was created without an id");
                return new User(keys.getLong(1), email, passwordHash, Role.STUDENT, true, Instant.now());
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Could not create user", ex);
        }
    }

    @Override public List<User> findAll() {
        try (var connection = connections.open(); var statement = connection.prepareStatement("SELECT id,email,password_hash,role,active,created_at FROM users ORDER BY created_at DESC"); var rows = statement.executeQuery()) {
            var result = new ArrayList<User>(); while (rows.next()) result.add(map(rows)); return result;
        } catch (SQLException ex) { throw new IllegalStateException("Could not load users", ex); }
    }

    @Override public void setActive(long id, boolean active) {
        try (var connection = connections.open(); var statement = connection.prepareStatement("UPDATE users SET active=? WHERE id=?")) { statement.setBoolean(1, active); statement.setLong(2, id); statement.executeUpdate(); }
        catch (SQLException ex) { throw new IllegalStateException("Could not update user", ex); }
    }

    private User map(java.sql.ResultSet result) throws SQLException {
        return new User(result.getLong("id"), result.getString("email"), result.getString("password_hash"), Role.valueOf(result.getString("role")), result.getBoolean("active"), result.getTimestamp("created_at").toInstant());
    }
}
