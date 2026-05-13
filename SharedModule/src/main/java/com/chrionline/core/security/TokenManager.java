package com.chrionline.core.security;

import com.chrionline.shared.models.Utilisateur;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestionnaire centralisé des tokens avec cycle de vie complet.
 *
 * Responsabilités:
 * - Création de tokens
 * - Validation des tokens (expiration, IP binding, révocation)
 * - Révocation (logout, changement de mot de passe)
 * - Nettoyage automatique des tokens expirés
 *
 * CORRECTIONS APPORTÉES:
 * 1. Singleton — une seule instance partagée dans toute l'application
 * 2. getValidSession() supprime immédiatement les tokens expirés détectés
 * 3. createToken() révoque les anciennes sessions du même utilisateur
 *    pour éviter l'accumulation de tokens valides simultanés
 */
public class TokenManager {
    private static final Logger logger = LoggerFactory.getLogger(TokenManager.class);

    // ─── CORRECTION 1 : Singleton ─────────────────────────────────────────────
    // AVANT : pas de Singleton → chaque new TokenManager() avait sa propre map
    //         → les tokens créés par le serveur étaient invisibles au dispatcher
    // APRÈS : une seule instance partagée garantit la cohérence des sessions
    private static final TokenManager INSTANCE = new TokenManager();

    /** Point d'accès global à l'instance unique. */
    public static TokenManager getInstance() {
        return INSTANCE;
    }

    /** Constructeur privé — utiliser getInstance() */
    private TokenManager() {}

    // ─── Configuration ────────────────────────────────────────────────────────

    private static final long DEFAULT_TTL_MINUTES      = TokenSession.TTL_DEFAULT_MINUTES;
    private static final long CLEANUP_INTERVAL_MILLIS  = 15 * 60 * 1000; // 15 minutes

    // ─── Stockage thread-safe ─────────────────────────────────────────────────

    /** Map principale : token UUID → TokenSession */
    private final Map<String, TokenSession> sessions = new ConcurrentHashMap<>();

    private long lastCleanupTime = System.currentTimeMillis();

    // ─── Création ─────────────────────────────────────────────────────────────

    /**
     * Crée et stocke un nouveau token pour l'utilisateur.
     *
     * CORRECTION 3 : révoque automatiquement les anciennes sessions
     * du même utilisateur pour éviter d'accumuler des tokens valides.
     *
     * @param utilisateur l'utilisateur authentifié
     * @param clientIp    l'adresse IP du client
     * @return le token UUID généré
     */
    public String createToken(Utilisateur utilisateur, String clientIp) {
        if (utilisateur == null || clientIp == null || clientIp.isBlank())
            throw new IllegalArgumentException("Utilisateur et IP requis");

        // CORRECTION 3 : révoquer les sessions existantes du même utilisateur
        // (évite qu'un utilisateur accumule plusieurs tokens valides en parallèle)
        revokeAllUserTokens(utilisateur.getId());

        String token = UUID.randomUUID().toString();
        TokenSession session = new TokenSession(token, utilisateur, clientIp, DEFAULT_TTL_MINUTES);
        sessions.put(token, session);

        logger.info("✅ Token créé: {}... | User: {} | IP: {} | Expire dans: {} min",
                token.substring(0, 8),
                utilisateur.getEmail(),
                clientIp,
                DEFAULT_TTL_MINUTES);

        tryCleanupExpiredTokens();
        return token;
    }

    // ─── Validation ───────────────────────────────────────────────────────────

    /**
     * Récupère une session valide après vérification complète.
     * Vérifie dans l'ordre : révocation → expiration → IP binding.
     *
     * CORRECTION 2 : les tokens expirés sont supprimés immédiatement
     * au lieu d'attendre le prochain cycle de nettoyage (15 min).
     *
     * @param token    le token à valider
     * @param clientIp l'IP actuelle du client
     * @return la session si valide, null sinon
     */
    public TokenSession getValidSession(String token, String clientIp) {
        if (token == null || token.isBlank()) return null;

        TokenSession session = sessions.get(token);
        if (session == null) {
            logger.warn("❌ Token non trouvé: {}...", token.substring(0, 8));
            return null;
        }

        // Vérification 1 — Révocation
        if (session.isRevoked()) {
            logger.warn("❌ Token révoqué: {}... | User: {}",
                    token.substring(0, 8), session.getUtilisateur().getEmail());
            return null;
        }

        // Vérification 2 — Expiration
        // CORRECTION 2 : suppression immédiate du token expiré de la map
        // AVANT : le token restait en mémoire jusqu'au nettoyage automatique (15 min)
        // APRÈS : libération mémoire immédiate + cohérence de la map
        if (session.isExpired()) {
            sessions.remove(token); // ← CORRECTION
            logger.warn("❌ Token expiré supprimé: {}... | User: {}",
                    token.substring(0, 8), session.getUtilisateur().getEmail());
            return null;
        }

        // Vérification 3 — IP Binding
        if (!session.isIpBindingValid(clientIp)) {
            logger.warn("❌ IP binding invalide: {}... | Attendu: {} | Reçu: {}",
                    token.substring(0, 8), session.getBoundIp(), clientIp);
            return null;
        }

        logger.debug("✅ Token valide: {}... | User: {} | Expire dans: {}s",
                token.substring(0, 8),
                session.getUtilisateur().getEmail(),
                session.getRemainingSeconds());

        return session;
    }

    /**
     * Valide un token sans vérifier l'IP (usage interne uniquement).
     * Vérifie quand même l'expiration et la révocation.
     *
     * @param token le token
     * @return la session si non expirée et non révoquée, null sinon
     */
    public TokenSession getSessionWithoutIpCheck(String token) {
        if (token == null || token.isBlank()) return null;

        TokenSession session = sessions.get(token);
        if (session == null) return null;

        if (session.isExpired()) {
            sessions.remove(token); // CORRECTION 2 appliquée ici aussi
            return null;
        }

        if (session.isRevoked()) return null;

        return session;
    }

    /**
     * Raccourci pour récupérer directement l'utilisateur d'un token valide.
     *
     * @param token    le token
     * @param clientIp l'IP du client
     * @return l'utilisateur, ou null si token invalide
     */
    public Utilisateur getUser(String token, String clientIp) {
        TokenSession session = getValidSession(token, clientIp);
        return session != null ? session.getUtilisateur() : null;
    }

    // ─── Révocation ───────────────────────────────────────────────────────────

    /**
     * Révoque un token unique (logout normal).
     *
     * @param token le token à révoquer
     */
    public void revokeToken(String token) {
        if (token == null || token.isBlank()) return;

        TokenSession session = sessions.get(token);
        if (session != null) {
            session.revoke();
            logger.info("🔒 Token révoqué (logout): {}... | User: {}",
                    token.substring(0, 8), session.getUtilisateur().getEmail());
        }
    }

    /**
     * Révoque tous les tokens d'un utilisateur.
     * Appelé lors d'un changement de mot de passe ou d'une suspension de compte.
     *
     * @param userId l'ID de l'utilisateur
     */
    public void revokeAllUserTokens(int userId) {
        int count = 0;
        for (TokenSession session : sessions.values()) {
            if (session.getUtilisateur().getId() == userId && !session.isRevoked()) {
                session.revoke();
                count++;
            }
        }
        if (count > 0) {
            logger.info("🔒 {} token(s) révoqué(s) pour userId={}", count, userId);
        }
    }

    // ─── Nettoyage ────────────────────────────────────────────────────────────

    /**
     * Nettoyage automatique déclenché à chaque création de token,
     * mais exécuté au plus une fois toutes les 15 minutes.
     */
    private void tryCleanupExpiredTokens() {
        long now = System.currentTimeMillis();
        if (now - lastCleanupTime < CLEANUP_INTERVAL_MILLIS) return;

        int before = sessions.size();
        sessions.values().removeIf(s -> s.isExpired() || s.isRevoked());
        int removed = before - sessions.size();

        if (removed > 0) {
            logger.info("🧹 Nettoyage: {} token(s) supprimé(s) ({} → {})",
                    removed, before, sessions.size());
        }

        lastCleanupTime = now;
    }

    /**
     * Force un nettoyage immédiat (tests ou maintenance).
     *
     * @return nombre de tokens supprimés
     */
    public int forceCleanup() {
        int before = sessions.size();
        sessions.values().removeIf(s -> s.isExpired() || s.isRevoked());
        lastCleanupTime = System.currentTimeMillis();
        return before - sessions.size();
    }

    // ─── Statistiques & Debug ─────────────────────────────────────────────────

    /** @return nombre total de sessions stockées (valides + expirées) */
    public int getActiveSessionCount() {
        return sessions.size();
    }

    /** @return nombre de sessions réellement valides */
    public int getValidSessionCount() {
        return (int) sessions.values().stream().filter(TokenSession::isValid).count();
    }

    /** @return true si le token est valide (sans vérification IP) */
    public boolean isTokenValid(String token) {
        TokenSession session = getSessionWithoutIpCheck(token);
        return session != null && session.isValid();
    }

    /**
     * Retourne des informations de debug sur un token.
     *
     * @param token le token
     * @return description lisible
     */
    public String getTokenInfo(String token) {
        if (token == null) return null;
        TokenSession session = sessions.get(token);
        if (session == null) return "Token non trouvé";
        return session + " | " + (session.isValid() ? "✅ VALIDE" : "❌ " + session.getInvalidityReason());
    }

    /**
     * Retourne les statistiques globales des sessions.
     *
     * @return map contenant les compteurs de sessions
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSessions",   sessions.size());
        stats.put("validSessions",   getValidSessionCount());
        stats.put("expiredSessions", sessions.values().stream().filter(TokenSession::isExpired).count());
        stats.put("revokedSessions", sessions.values().stream().filter(TokenSession::isRevoked).count());
        return stats;
    }

    /**
     * Vide complètement le gestionnaire.
     * ATTENTION : réservé aux tests unitaires uniquement.
     */
    public void clear() {
        sessions.clear();
        logger.warn("⚠️ TokenManager vidé complètement (usage test uniquement)");
    }
}