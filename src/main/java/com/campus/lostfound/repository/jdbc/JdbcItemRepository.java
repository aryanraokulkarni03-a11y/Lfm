package com.campus.lostfound.repository.jdbc;

import com.campus.lostfound.config.ConnectionProvider;
import com.campus.lostfound.domain.enums.ItemStatus;
import com.campus.lostfound.domain.enums.ReportType;
import com.campus.lostfound.domain.model.Item;
import com.campus.lostfound.repository.api.ItemRepository;

import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcItemRepository implements ItemRepository {
    private final ConnectionProvider connections;
    public JdbcItemRepository(ConnectionProvider connections) { this.connections = connections; }

    @Override public Item save(Item item) {
        String sql = "INSERT INTO items (reporter_id, report_type, title, description, category, location, event_date, identifying_marks, contact_details) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (var connection = connections.open(); var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, item.reporterId()); statement.setString(2, item.reportType().name());
            statement.setString(3, item.title().trim()); statement.setString(4, item.description().trim());
            statement.setString(5, item.category().trim()); statement.setString(6, item.location().trim());
            statement.setObject(7, item.eventDate()); statement.setString(8, item.identifyingMarks().trim());
            statement.setString(9, item.contactDetails().trim()); statement.executeUpdate();
            try (var keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new IllegalStateException("Item was created without an id");
                return new Item(keys.getLong(1), item.reporterId(), item.reporterEmail(), item.reportType(), item.title(), item.description(), item.category(), item.location(), item.eventDate(), item.identifyingMarks(), item.contactDetails(), ItemStatus.OPEN, item.imagePaths());
            }
        } catch (SQLException ex) { throw new IllegalStateException("Could not create item", ex); }
    }

    @Override public List<Item> search(String keyword, String category, ReportType type, ItemStatus status, boolean includeArchived) {
        StringBuilder sql = new StringBuilder("SELECT i.*, u.email reporter_email FROM items i JOIN users u ON u.id=i.reporter_id WHERE 1=1");
        List<Object> parameters = new ArrayList<>();
        if (!includeArchived) sql.append(" AND i.status <> 'ARCHIVED'");
        if (keyword != null && !keyword.isBlank()) { sql.append(" AND (LOWER(i.title) LIKE ? OR LOWER(i.description) LIKE ? OR LOWER(i.location) LIKE ?)"); String value = "%" + keyword.trim().toLowerCase() + "%"; parameters.add(value); parameters.add(value); parameters.add(value); }
        if (category != null && !category.isBlank() && !category.equals("All categories")) { sql.append(" AND i.category = ?"); parameters.add(category); }
        if (type != null) { sql.append(" AND i.report_type = ?"); parameters.add(type.name()); }
        if (status != null) { sql.append(" AND i.status = ?"); parameters.add(status.name()); }
        sql.append(" ORDER BY i.created_at DESC");
        try (var connection = connections.open(); var statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < parameters.size(); i++) statement.setObject(i + 1, parameters.get(i));
            try (var result = statement.executeQuery()) { var output = new ArrayList<Item>(); while (result.next()) output.add(map(result, connection)); return output; }
        } catch (SQLException ex) { throw new IllegalStateException("Could not search items", ex); }
    }

    @Override public Optional<Item> findById(long id) {
        try (var connection = connections.open(); var statement = connection.prepareStatement("SELECT i.*, u.email reporter_email FROM items i JOIN users u ON u.id=i.reporter_id WHERE i.id=?")) {
            statement.setLong(1, id); try (var result = statement.executeQuery()) { return result.next() ? Optional.of(map(result, connection)) : Optional.empty(); }
        } catch (SQLException ex) { throw new IllegalStateException("Could not load item", ex); }
    }

    @Override public void updateStatus(long id, ItemStatus status) { executeStatus(id, status); }
    @Override public void archive(long id) { executeStatus(id, ItemStatus.ARCHIVED); }

    private void executeStatus(long id, ItemStatus status) {
        try (var connection = connections.open(); var statement = connection.prepareStatement("UPDATE items SET status=? WHERE id=?")) { statement.setString(1, status.name()); statement.setLong(2, id); statement.executeUpdate(); }
        catch (SQLException ex) { throw new IllegalStateException("Could not update item status", ex); }
    }

    private Item map(java.sql.ResultSet result, java.sql.Connection connection) throws SQLException {
        long id = result.getLong("id");
        var paths = new ArrayList<String>();
        try (var images = connection.prepareStatement("SELECT file_path FROM item_images WHERE item_id=? ORDER BY display_order")) { images.setLong(1, id); try (var rows = images.executeQuery()) { while (rows.next()) paths.add(rows.getString(1)); } }
        LocalDate date = result.getObject("event_date", LocalDate.class);
        return new Item(id, result.getLong("reporter_id"), result.getString("reporter_email"), ReportType.valueOf(result.getString("report_type")), result.getString("title"), result.getString("description"), result.getString("category"), result.getString("location"), date, result.getString("identifying_marks"), result.getString("contact_details"), ItemStatus.valueOf(result.getString("status")), paths);
    }
}
