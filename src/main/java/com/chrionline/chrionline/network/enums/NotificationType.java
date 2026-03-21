package com.chrionline.chrionline.network.enums;

public enum NotificationType {
    INFO,
    SYSTEM,
    USER,
    RESERVATION,
    PAYMENT,
    SECURITY,
    NETWORK,
    APPLICATION,
    ERROR,
    UNKNOWN;


    public static NotificationType fromString(String value) {
        for (NotificationType type : values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
