package com.chrionline.network.enums;

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
    ORDER_CONFIRMED,
    ORDER_CANCELLED,
    STOCK_UPDATE,
    PAYMENT_FAILED,
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
