package com.chrionline.chrionline.network.enums;

public enum RequestType {
    COMMAND,        // Standard command/action
    QUERY,          // Data retrieval
    SUBSCRIBE,      // Subscribe to events
    UNSUBSCRIBE,    // Unsubscribe from events
    PING,           // Heartbeat/ping
    AUTHENTICATE    // Authentication request
}
