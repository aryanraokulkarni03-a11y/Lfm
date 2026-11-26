package com.campus.lostfound.repository.jdbc;

import com.campus.lostfound.config.ConnectionProvider;
import com.campus.lostfound.domain.enums.ClaimStatus;
import com.campus.lostfound.domain.model.Claim;
import com.campus.lostfound.repository.api.ClaimRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class JdbcClaimRepository implements ClaimRepository {
    private final ConnectionProvider connections;

    public JdbcClaimRepository(ConnectionProvider connections) {
        this.connections = connections;
    }

    @Override
    public Claim save(Claim claim) {
        String sql = "INSERT INTO claims (item_id, claimant_id, ownership_explanation, "
                + "identifying_details, possession_information, additional_notes, evidence_path) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (var connection = connections.open();
             var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, claim.itemId());
            statement.setLong(2, claim.claimantId());
            statement.setString(3, claim.ownershipExplanation());
            statement.setString(4, claim.identifyingDetails());
            statement.setString(5, claim.possessionInformation());
            statement.setString(6, claim.additionalNotes());
            statement.setString(7, claim.evidencePath());
            statement.executeUpdate();

            try (var keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new IllegalStateException("Claim was created without an id");
                }

                try (var status = connection.prepareStatement(
                        "UPDATE items SET status='CLAIM_PENDING' WHERE id=?")) {
                    status.setLong(1, claim.itemId());
                    status.executeUpdate();
                }

                return new Claim(keys.getLong(1), claim.itemId(), claim.itemTitle(),
                        claim.claimantId(), claim.claimantEmail(), claim.ownershipExplanation(),
                        claim.identifyingDetails(), claim.possessionInformation(),
                        claim.additionalNotes(), claim.evidencePath(), ClaimStatus.PENDING,
                        null, java.time.Instant.now());
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Could not submit claim", ex);
        }
    }

    @Override
    public boolean hasActiveClaim(long itemId) {
        String sql = "SELECT COUNT(*) FROM claims WHERE item_id=? AND status IN ('PENDING','APPROVED')";
        try (var connection = connections.open();
             var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, itemId);
            try (var rows = statement.executeQuery()) {
                rows.next();
                return rows.getInt(1) > 0;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Could not check claims", ex);
        }
    }

    @Override
    public List<Claim> findByClaimant(long claimantId) {
        return find("WHERE c.claimant_id=?", claimantId);
    }

    @Override
    public List<Claim> findPending() {
        return find("WHERE c.status='PENDING'");
    }

    @Override
    public List<Claim> findAll() {
        return find("");
    }

    private List<Claim> find(String where, Object... args) {
        String sql = "SELECT c.*, i.title item_title, u.email claimant_email "
                + "FROM claims c JOIN items i ON i.id=c.item_id "
                + "JOIN users u ON u.id=c.claimant_id " + where + " ORDER BY c.created_at DESC";
        try (var connection = connections.open();
             var statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                statement.setObject(i + 1, args[i]);
            }

            try (var rows = statement.executeQuery()) {
                var result = new ArrayList<Claim>();
                while (rows.next()) {
                    result.add(map(rows));
                }
                return result;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Could not load claims", ex);
        }
    }

    @Override
    public void review(long claimId, long reviewerId, ClaimStatus status, String reason) {
        String select = "SELECT c.item_id FROM claims c JOIN users u ON u.id=? WHERE c.id=? AND c.status='PENDING' AND u.role='ADMIN' AND u.active=TRUE";
        try (var connection = connections.open()) {
            connection.setAutoCommit(false);
            try (var lookup = connection.prepareStatement(select)) {
                lookup.setLong(1, reviewerId);
                lookup.setLong(2, claimId);
                try (var rows = lookup.executeQuery()) {
                    if (!rows.next()) {
                        throw new IllegalStateException("This claim is no longer pending");
                    }
                    long itemId = rows.getLong("item_id");
                    String updateClaim = "UPDATE claims SET status=?, reviewed_by=?, review_reason=?, "
                            + "reviewed_at=CURRENT_TIMESTAMP WHERE id=?";
                    try (var update = connection.prepareStatement(updateClaim)) {
                        update.setString(1, status.name());
                        update.setLong(2, reviewerId);
                        update.setString(3, reason);
                        update.setLong(4, claimId);
                        update.executeUpdate();
                    }

                    String itemStatus = status == ClaimStatus.APPROVED ? "CLAIM_APPROVED" : "OPEN";
                    try (var update = connection.prepareStatement(
                            "UPDATE items SET status=? WHERE id=?")) {
                        update.setString(1, itemStatus);
                        update.setLong(2, itemId);
                        update.executeUpdate();
                    }
                    connection.commit();
                }
            } catch (RuntimeException | SQLException ex) {
                connection.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Could not review claim", ex);
        }
    }

    private Claim map(java.sql.ResultSet rows) throws SQLException {
        return new Claim(
                rows.getLong("id"),
                rows.getLong("item_id"),
                rows.getString("item_title"),
                rows.getLong("claimant_id"),
                rows.getString("claimant_email"),
                rows.getString("ownership_explanation"),
                rows.getString("identifying_details"),
                rows.getString("possession_information"),
                rows.getString("additional_notes"),
                rows.getString("evidence_path"),
                ClaimStatus.valueOf(rows.getString("status")),
                rows.getString("review_reason"),
                rows.getTimestamp("created_at").toInstant());
    }

}
