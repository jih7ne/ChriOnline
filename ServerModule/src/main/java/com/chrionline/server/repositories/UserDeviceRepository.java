package com.chrionline.server.repositories;

import com.chrionline.shared.models.UserDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for the user_devices table.
 *   id, user_email, device_name, public_key, fingerprint,
 *   key_algorithm, created_at, last_used_at, revoked, revoked_at
 */
public class UserDeviceRepository {

    private static final Logger logger = LoggerFactory.getLogger(UserDeviceRepository.class);
    private final Connection connection;

    public UserDeviceRepository(Connection connection) {
        this.connection = connection;
    }

    // ── INSERT ────────────────────────────────────────────────────────────

    /**
     * Register a new device for a user.
     * Sets created_at = NOW(), revoked = false.
     */
    public boolean add(UserDevice device) {
        String sql = """
            INSERT INTO user_devices
                (user_email, device_name, public_key, fingerprint, key_algorithm, created_at, revoked)
            VALUES (?, ?, ?, ?, ?, NOW(), FALSE)
            """;
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, device.getUserEmail());
            stmt.setString(2, device.getDeviceName());
            stmt.setString(3, device.getPublicKey());
            stmt.setString(4, device.getFingerprint());
            stmt.setString(5, device.getKeyAlgorithm() != null ? device.getKeyAlgorithm() : "RSA");
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                ResultSet keys = stmt.getGeneratedKeys();
                if (keys.next()) device.setId(keys.getInt(1));
                logger.info("Device registered: {} for {}", device.getDeviceName(), device.getUserEmail());
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error adding device: {}", e.getMessage());
        }
        return false;
    }

    // ── READ ──────────────────────────────────────────────────────────────

    /**
     * Get all non-revoked devices for a given user email.
     */
    public List<UserDevice> getActiveDevicesByEmail(String email) {
        String sql = """
            SELECT * FROM user_devices
            WHERE user_email = ? AND revoked = FALSE
            ORDER BY created_at DESC
            """;
        List<UserDevice> list = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            logger.error("Error fetching devices for {}: {}", email, e.getMessage());
        }
        return list;
    }

    public boolean hasKeys(String email) {
        String sql = """
            SELECT count(*) FROM user_devices where  user_email = ? AND revoked = FALSE
        """;

        try(PreparedStatement stmt = connection.prepareStatement(sql)){
            stmt.setString(1, email);
            ResultSet rs =  stmt.executeQuery();
            if (rs.next()) { return rs.getInt(1) > 0; }
        }catch (SQLException e){
            logger.error("Error fetching keys for {}: {}", email, e.getMessage());
        }
        return false;
    }

    public boolean isAdmin(String email) {
        String sql = """
            SELECT role from utilisateur where email = ?
        """;

        try(PreparedStatement stmt = connection.prepareStatement(sql)){
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String role = rs.getString(1);
                return role.equalsIgnoreCase("ADMIN");
            }
        }catch (SQLException e){
            logger.error("Error checking if administrator {}: {}", email, e.getMessage());
        }
        return false;
    }

    /**
     * Get ALL devices (including revoked) for a user — needed for the settings UI.
     */
    public List<UserDevice> getAllDevicesByEmail(String email) {
        String sql = "SELECT * FROM user_devices WHERE user_email = ? ORDER BY created_at DESC";
        List<UserDevice> list = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            logger.error("Error fetching all devices for {}: {}", email, e.getMessage());
        }
        return list;
    }

    /**
     * Find a device by its fingerprint (unique).
     * Used during key-based authentication challenge.
     */
    public UserDevice getByFingerprint(String fingerprint) {
        String sql = "SELECT * FROM user_devices WHERE fingerprint = ? AND revoked = FALSE";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, fingerprint);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            logger.error("Error fetching device by fingerprint: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Check if a fingerprint is already registered (to avoid duplicates).
     */
    public boolean fingerprintExists(String fingerprint) {
        String sql = "SELECT COUNT(*) FROM user_devices WHERE fingerprint = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, fingerprint);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            logger.error("Error checking fingerprint: {}", e.getMessage());
        }
        return false;
    }

    // ── UPDATE ────────────────────────────────────────────────────────────

    /**
     * Update last_used_at to NOW() for the given fingerprint.
     * Called after a successful key-based authentication.
     */
    public void updateLastUsed(String fingerprint) {
        String sql = "UPDATE user_devices SET last_used_at = NOW() WHERE fingerprint = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, fingerprint);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating last_used for {}: {}", fingerprint, e.getMessage());
        }
    }

    // ── REVOKE ────────────────────────────────────────────────────────────

    /**
     * Revoke a device by its ID. Sets revoked=true and revoked_at=NOW().
     * Does NOT delete — keeps audit trail.
     */
    public boolean revokeById(int id) {
        String sql = "UPDATE user_devices SET revoked = TRUE, revoked_at = NOW() WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            boolean ok = stmt.executeUpdate() > 0;
            if (ok) logger.info("Device id={} revoked", id);
            return ok;
        } catch (SQLException e) {
            logger.error("Error revoking device id={}: {}", id, e.getMessage());
        }
        return false;
    }

    /**
     * Revoke a device by its fingerprint.
     */
    public boolean revokeByFingerprint(String fingerprint) {
        String sql = "UPDATE user_devices SET revoked = TRUE, revoked_at = NOW() WHERE fingerprint = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, fingerprint);
            boolean ok = stmt.executeUpdate() > 0;
            if (ok) logger.info("Device fingerprint={} revoked", fingerprint);
            return ok;
        } catch (SQLException e) {
            logger.error("Error revoking device: {}", e.getMessage());
        }
        return false;
    }


    public String getPublicKey(String fingerprint) {
        String sql = "SELECT public_key FROM user_devices WHERE fingerprint = ? and  revoked = FALSE";

        try(PreparedStatement stmt = connection.prepareStatement(sql)){
            stmt.setString(1, fingerprint);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getString(1);
        }catch (SQLException e){
            logger.error("Error getting public key for {}: {}", fingerprint, e.getMessage());
        }
        return null;
    }

    // ── ROW MAPPER ────────────────────────────────────────────────────────

    private UserDevice mapRow(ResultSet rs) throws SQLException {
        UserDevice d = new UserDevice();
        d.setId(rs.getInt("id"));
        d.setUserEmail(rs.getString("user_email"));
        d.setDeviceName(rs.getString("device_name"));
        d.setPublicKey(rs.getString("public_key"));
        d.setFingerprint(rs.getString("fingerprint"));
        d.setKeyAlgorithm(rs.getString("key_algorithm"));
        d.setRevoked(rs.getBoolean("revoked"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) d.setCreatedAt(createdAt.toLocalDateTime());

        Timestamp lastUsed = rs.getTimestamp("last_used_at");
        if (lastUsed != null) d.setLastUsedAt(lastUsed.toLocalDateTime());

        Timestamp revokedAt = rs.getTimestamp("revoked_at");
        if (revokedAt != null) d.setRevokedAt(revokedAt.toLocalDateTime());

        return d;
    }



}