package com.chrionline.shared.models;

import java.time.LocalDateTime;

/**
 * Represents a device registered by a user for key-based authentication.
 * Maps to the user_devices table.
 *
 * Flow:
 *   private key → derive → public key → SHA-256 → fingerprint
 *   The private key lives ONLY on the client machine (binary file, never sent).
 *   Only public_key + fingerprint are stored server-side.
 */
public class UserDevice {

    private int id;
    private String userEmail;
    private String deviceName;
    private String publicKey;       // Base64-encoded RSA public key
    private String fingerprint;     // SHA-256 of the public key (hex)
    private String keyAlgorithm;    // e.g. "RSA"
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
    private boolean revoked;
    private LocalDateTime revokedAt;

    public UserDevice() {}

    public UserDevice(String userEmail, String deviceName, String publicKey,
                      String fingerprint, String keyAlgorithm) {
        this.userEmail    = userEmail;
        this.deviceName   = deviceName;
        this.publicKey    = publicKey;
        this.fingerprint  = fingerprint;
        this.keyAlgorithm = keyAlgorithm != null ? keyAlgorithm : "RSA";
        this.revoked      = false;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }

    public String getFingerprint() { return fingerprint; }
    public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }

    public String getKeyAlgorithm() { return keyAlgorithm; }
    public void setKeyAlgorithm(String keyAlgorithm) { this.keyAlgorithm = keyAlgorithm; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }

    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }

    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }

    /**
     * Returns a short display label for UI: first 8 chars of fingerprint in pairs.
     * e.g. "SHA256:AB:34:9F:..."
     */
    public String getShortFingerprint() {
        if (fingerprint == null || fingerprint.length() < 8) return fingerprint;
        // Format like SSH: SHA256:AB:34:9F:...
        StringBuilder sb = new StringBuilder("SHA256:");
        for (int i = 0; i < Math.min(fingerprint.length(), 16); i += 2) {
            if (i > 0) sb.append(":");
            sb.append(fingerprint, i, Math.min(i + 2, fingerprint.length()));
        }
        sb.append("...");
        return sb.toString().toUpperCase();
    }

    @Override
    public String toString() {
        return "UserDevice{id=" + id + ", email='" + userEmail + "', device='" + deviceName
                + "', fingerprint='" + getShortFingerprint() + "', revoked=" + revoked + "}";
    }
}