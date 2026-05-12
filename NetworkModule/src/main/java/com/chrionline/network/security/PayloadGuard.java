package com.chrionline.network.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gardien de la taille des payloads (protection contre les DoS).
 * Limite la taille des messages entrants à 64 KB maximum.
 */
public class PayloadGuard {

    private static final Logger logger = LoggerFactory.getLogger(PayloadGuard.class);
    
    // 64 KB = 64 * 1024 bytes
    public static final int MAX_PAYLOAD_SIZE_BYTES = 64 * 1024;

    /**
     * Vérifie si la taille du message est dans les limites autorisées.
     * @param message Le message reçu sous forme de chaîne de caractères.
     * @return true si le payload est valide, false s'il est trop grand.
     */
    public static boolean isPayloadValid(String message) {
        if (message == null) {
            return true;
        }
        
        // Estimer la taille en octets (UTF-8)
        // message.length() donne le nombre de caractères. En UTF-8, ça peut être plus.
        // On utilise la méthode exacte getBytes pour plus de précision.
        int sizeInBytes = message.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        
        if (sizeInBytes > MAX_PAYLOAD_SIZE_BYTES) {
            logger.warn("Payload rejeté ! Taille : {} octets (Max: {} octets)", sizeInBytes, MAX_PAYLOAD_SIZE_BYTES);
            return false;
        }
        
        return true;
    }
}
