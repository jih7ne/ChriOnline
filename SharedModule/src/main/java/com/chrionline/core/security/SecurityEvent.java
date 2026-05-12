package com.chrionline.core.security;

import java.time.LocalDateTime;

public class SecurityEvent {
    
    public enum EventType {
        LOGIN_SUCCESS,
        LOGIN_FAILED,
        ACCOUNT_BLOCKED,
        RATE_LIMIT_EXCEEDED,
        PAYLOAD_TOO_LARGE,
        INVALID_HMAC
    }

    private EventType type;
    private String ipAddress;
    private String userEmail;
    private String description;
    private LocalDateTime timestamp;

    public SecurityEvent(EventType type, String ipAddress, String userEmail, String description) {
        this.type = type;
        this.ipAddress = ipAddress;
        this.userEmail = userEmail;
        this.description = description;
        this.timestamp = LocalDateTime.now();
    }

    // Getters
    public EventType getType() { return type; }
    public String getIpAddress() { return ipAddress; }
    public String getUserEmail() { return userEmail; }
    public String getDescription() { return description; }
    public LocalDateTime getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return String.format("[%s] %s - IP: %s - User: %s - %s",
                timestamp, type, ipAddress, userEmail != null ? userEmail : "N/A", description);
    }
}
