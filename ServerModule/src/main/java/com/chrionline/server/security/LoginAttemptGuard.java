package com.chrionline.server.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gère la limitation des tentatives de connexion par adresse IP.
 *
 * Stockage : cache en mémoire (ConcurrentHashMap) — aucune dépendance BD.
 *
 * Règles :
 *  - 3 tentatives échouées consécutives → IP bloquée
 *  - Durée du blocage : LOCK_MINUTES minutes
 *  - La fenêtre de comptage est glissante : si la dernière tentative remonte
 *    à plus de WINDOW_MINUTES minutes, le compteur est remis à zéro.
 *  - Un succès réinitialise immédiatement le compteur.
 *
 * Remarque : le cache est lié au cycle de vie de la JVM.
 * Un redémarrage du serveur remet tous les compteurs à zéro.
 */
public class LoginAttemptGuard {

    private static final Logger logger = LoggerFactory.getLogger(LoginAttemptGuard.class);

    public static final int MAX_ATTEMPTS   = 3;
    public static final int LOCK_MINUTES   = 15;
    public static final int WINDOW_MINUTES = 15;

    // ── Structure interne ─────────────────────────────────────────────────

    /**
     * Enregistrement immuable représentant l'état d'une IP.
     */
    private record AttemptRecord(
            int attempts,
            LocalDateTime lastAttempt,
            LocalDateTime lockedUntil
    ) {}

    /** Cache principal : ip → état des tentatives. */
    private final ConcurrentHashMap<String, AttemptRecord> cache = new ConcurrentHashMap<>();

    // ── API publique ───────────────────────────────────────────────────────

    /**
     * Vérifie si l'IP est actuellement bloquée.
     *
     * @return true si l'IP est bloquée (la requête doit être rejetée).
     */
    public boolean isBlocked(String ip) {
        if (ip == null || ip.isBlank()) return false;

        AttemptRecord rec = cache.get(ip);
        if (rec == null) return false;

        // Fenêtre de comptage expirée → on nettoie et on laisse passer
        if (rec.lastAttempt().isBefore(LocalDateTime.now().minusMinutes(WINDOW_MINUTES))) {
            cache.remove(ip);
            return false;
        }

        if (rec.lockedUntil() == null) return false;

        // Verrou expiré → on nettoie et on laisse passer
        if (LocalDateTime.now().isAfter(rec.lockedUntil())) {
            cache.remove(ip);
            return false;
        }

        logger.warn("IP bloquée : {} (jusqu'à {})", ip, rec.lockedUntil());
        return true;
    }

    /**
     * Retourne le nombre de minutes restantes de blocage (0 si non bloquée).
     */
    public long minutesRemaining(String ip) {
        AttemptRecord rec = cache.get(ip);
        if (rec == null || rec.lockedUntil() == null) return 0;
        if (LocalDateTime.now().isAfter(rec.lockedUntil())) return 0;
        return Duration.between(LocalDateTime.now(), rec.lockedUntil()).toMinutes() + 1;
    }

    /**
     * Enregistre un échec de connexion.
     * Bloque l'IP si le seuil est atteint.
     *
     * @return true si l'IP vient d'être bloquée à la suite de cet enregistrement.
     */
    public boolean recordFailure(String ip) {
        if (ip == null || ip.isBlank()) return false;

        // On récupère l'état existant ou on part de zéro
        AttemptRecord current = cache.getOrDefault(ip,
                new AttemptRecord(0, LocalDateTime.now(), null));

        int newAttempts = current.attempts() + 1;
        LocalDateTime lockedUntil = (newAttempts >= MAX_ATTEMPTS)
                ? LocalDateTime.now().plusMinutes(LOCK_MINUTES)
                : null;

        cache.put(ip, new AttemptRecord(newAttempts, LocalDateTime.now(), lockedUntil));

        if (newAttempts >= MAX_ATTEMPTS) {
            logger.warn("IP {} bloquée après {} tentatives échouées.", ip, newAttempts);
            return true;
        }

        logger.info("Échec de connexion depuis {} ({}/{})", ip, newAttempts, MAX_ATTEMPTS);
        return false;
    }

    /**
     * Réinitialise le compteur après un succès de connexion.
     */
    public void recordSuccess(String ip) {
        if (ip == null || ip.isBlank()) return;
        cache.remove(ip);
        logger.debug("Compteur réinitialisé pour IP {} (connexion réussie)", ip);
    }

    /**
     * Retourne le nombre de tentatives échouées actuelles pour cette IP.
     */
    public int getAttempts(String ip) {
        AttemptRecord rec = cache.get(ip);
        return rec != null ? rec.attempts() : 0;
    }
}
