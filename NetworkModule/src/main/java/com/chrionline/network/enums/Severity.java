package com.chrionline.network.enums;

public enum Severity {
    TRACE,
    DEBUG,
    INFO,
    WARNING,
    ERROR,
    LOW,
    HIGH,
    CRITICAL;


    public static Severity fromString(String value) {
        for (Severity severity : values()) {
            if (severity.name().equalsIgnoreCase(value)) {
                return severity;
            }
        }
        return ERROR;
    }

    public boolean isError() {
        return this == ERROR || this == CRITICAL;
    }
}
