package com.campus.lostfound.repository.jdbc;

import com.campus.lostfound.config.ConnectionProvider;
import com.campus.lostfound.repository.api.ItemImageRepository;

import java.sql.SQLException;
import java.util.List;

public final class JdbcItemImageRepository implements ItemImageRepository {
    private final ConnectionProvider connections;
    public JdbcItemImageRepository(ConnectionProvider connections) { this.connections = connections; }
    @Override public void save(long itemId, List<String> paths) {
        if (paths == null || paths.isEmpty()) return;
        try (var connection = connections.open(); var statement = connection.prepareStatement("INSERT INTO item_images (item_id, file_path, display_order) VALUES (?, ?, ?)")) {
            for (int i = 0; i < paths.size(); i++) { statement.setLong(1, itemId); statement.setString(2, paths.get(i)); statement.setInt(3, i); statement.addBatch(); }
            statement.executeBatch();
        } catch (SQLException ex) { throw new IllegalStateException("Could not save item images", ex); }
    }
}
