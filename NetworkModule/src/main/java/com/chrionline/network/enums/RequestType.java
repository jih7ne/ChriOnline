package com.chrionline.network.enums;

public enum RequestType {
    COMMAND,        // Standard command/action
    QUERY,          // Data retrieval
    SUBSCRIBE,      // Subscribe to events
    UNSUBSCRIBE,    // Unsubscribe from events
    PING,           // Heartbeat/ping
    AUTHENTICATE,    // Authentication request

    // 2FA
    TWO_FACTOR_VERIFY,   // valider le code après login
    TWO_FACTOR_ENABLE,   // activer le 2FA (retourne QR code)
    TWO_FACTOR_DISABLE,  // désactiver le 2FA
    TWO_FACTOR_STATUS   // vérifier si 2FA est activé
}
