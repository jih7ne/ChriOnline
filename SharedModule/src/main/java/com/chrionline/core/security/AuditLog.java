package com.chrionline.core.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Service pour journaliser les événements de sécurité et détecter des anomalies.
 */
public class AuditLog {

    private static final Logger logger = LoggerFactory.getLogger(AuditLog.class);
    private static final String AUDIT_FILE = "security_audit.log";
    private static final Queue<SecurityEvent> recentEvents = new ConcurrentLinkedQueue<>();
    private static final int MAX_RECENT_EVENTS = 1000;

    /**
     * Enregistre un événement de sécurité.
     */
    public static void logEvent(SecurityEvent.EventType type, String ipAddress, String userEmail, String description) {
        SecurityEvent event = new SecurityEvent(type, ipAddress, userEmail, description);
        
        // Log console
        logger.info("[AUDIT] {}", event);
        
        // Log fichier
        writeToDiskAsync(event);
        
        // Garder en mémoire pour détection comportementale simple
        recentEvents.offer(event);
        if (recentEvents.size() > MAX_RECENT_EVENTS) {
            recentEvents.poll();
        }
        
        analyzeBehavior(ipAddress);
    }

    private static void writeToDiskAsync(SecurityEvent event) {
        new Thread(() -> {
            try (FileWriter fw = new FileWriter(AUDIT_FILE, true);
                 PrintWriter pw = new PrintWriter(fw)) {
                pw.println(event.toString());
            } catch (IOException e) {
                logger.error("Impossible d'écrire dans le fichier d'audit", e);
            }
        }).start();
    }

    /**
     * Détection comportementale basique.
     * Si une IP génère beaucoup d'événements suspects, on peut lancer une alerte.
     */
    private static void analyzeBehavior(String ipAddress) {
        if (ipAddress == null) return;
        
        long suspiciousCount = recentEvents.stream()
                .filter(e -> ipAddress.equals(e.getIpAddress()))
                .filter(e -> e.getType() == SecurityEvent.EventType.LOGIN_FAILED ||
                             e.getType() == SecurityEvent.EventType.INVALID_HMAC ||
                             e.getType() == SecurityEvent.EventType.PAYLOAD_TOO_LARGE)
                .count();

        if (suspiciousCount >= 10) {
            logger.warn("[ALERTE] Comportement suspect détecté pour l'IP : {}", ipAddress);
            // On pourrait ici déclencher un blocage au niveau du RateLimiter ou LoginAttemptGuard
        }
    }
}
