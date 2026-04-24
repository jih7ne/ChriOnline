package com.chrionline.server.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  GESTIONNAIRE DE TOKENS DE RÉINITIALISATION SÉCURISÉ
 * 
 * Responsabilités:
 * - Génération de tokens uniques et impossibles à prédire
 * - Expiration automatique après 10 minutes
 * - Utilisabilité unique (invalidation après utilisation)
 * - Nettoyage automatique des tokens expirés
 * 
 * Flux sécurisé:
 * 1. User vérifie sa réponse à la question secrète → reçoit un token temporaire
 * 2. User envoie ce token + nouveau mot de passe → mot de passe réinitialisé
 * 3. Token est invalidé immédiatement après utilisation
 * 4. Tokens expirés (10 min) sont supprimés automatiquement
 */
public class ResetTokenStore {
    private static final Logger logger = LoggerFactory.getLogger(ResetTokenStore.class);

    // ── Configuration ────────────────────────────────────────────────────
    private static final long TOKEN_VALIDITY_MINUTES = 10;
    private static final long TOKEN_VALIDITY_MILLIS = TOKEN_VALIDITY_MINUTES * 60 * 1000;
    private static final int TOKEN_LENGTH_BYTES = 32;
    
    // ── Stockage des tokens (userId → token)
    // Structure: { token → { userId, expirationTime, used } }
    private static final Map<String, TokenInfo> tokenStore = new ConcurrentHashMap<>();
    private static final SecureRandom secureRandom = new SecureRandom();
    
    // ── Singleton ────────────────────────────────────────────────────────
    private static ResetTokenStore instance = null;
    
    private ResetTokenStore() {
        logger.info(" ResetTokenStore initialisé | Validité: {} min | Nettoyage auto activé",
                TOKEN_VALIDITY_MINUTES);
        startCleanupThread();
    }
    
    public static synchronized ResetTokenStore getInstance() {
        if (instance == null) {
            instance = new ResetTokenStore();
        }
        return instance;
    }
    
    // ── Structure interne pour stocker les info de token ───────────────────
    private static class TokenInfo {
        int userId;
        long expirationTime;
        boolean used;
        
        TokenInfo(int userId, long expirationTime) {
            this.userId = userId;
            this.expirationTime = expirationTime;
            this.used = false;
        }
    }
    
    // ── GÉNÉRER UN TOKEN À USAGE UNIQUE ──────────────────────────────────
    /**
     * Génère un token aléatoire et sécurisé valable 10 minutes.
     * Un seul token peut être actif par utilisateur à la fois.
     * 
     * @param userId l'ID de l'utilisateur qui initie la réinitialisation
     * @return token aléatoire et sécurisé (hex string)
     */
    public String generateToken(int userId) {
        // 1 — Invalider tout ancien token pour cet utilisateur
        invalidateTokensForUser(userId);
        
        // 2 — Générer un nouveau token aléatoire (32 bytes = 256 bits)
        byte[] randomBytes = new byte[TOKEN_LENGTH_BYTES];
        secureRandom.nextBytes(randomBytes);
        String token = bytesToHex(randomBytes);
        
        // 3 — Enregistrer le token avec expiration
        long expirationTime = System.currentTimeMillis() + TOKEN_VALIDITY_MILLIS;
        tokenStore.put(token, new TokenInfo(userId, expirationTime));
        
        logger.info(
                " Token de réinitialisation généré | UserId: {} | Validité: {} min",
                userId, TOKEN_VALIDITY_MINUTES
        );
        
        return token;
    }
    
    // ── VALIDER UN TOKEN ─────────────────────────────────────────────────
    /**
     * Valide un token et retourne l'ID utilisateur s'il est valide.
     * Le token est ensuite marqué comme utilisé (non réutilisable).
     * 
     * @param token le token à valider
     * @return l'ID utilisateur si valide, -1 sinon
     */
    public int validateAndConsumeToken(String token) {
        if (token == null || token.isEmpty()) {
            logger.warn(" Tentative de validation avec token null ou vide");
            return -1;
        }
        
        TokenInfo info = tokenStore.get(token);
        
        // Token n'existe pas
        if (info == null) {
            logger.warn(" Token invalide ou inexistant");
            return -1;
        }
        
        // Token déjà utilisé
        if (info.used) {
            logger.warn(" Tentative de réutilisation de token (UserId: {})", info.userId);
            return -1;
        }
        
        // Token expiré
        if (System.currentTimeMillis() > info.expirationTime) {
            logger.warn(" Token expiré (UserId: {}) | Expiration: {}",
                    info.userId, new Date(info.expirationTime));
            tokenStore.remove(token);
            return -1;
        }
        
        //  Token valide - on le marque comme utilisé
        info.used = true;
        logger.info(" Token validé et consommé | UserId: {}", info.userId);
        
        return info.userId;
    }
    
    // ── INVALIDER LES TOKENS D'UN UTILISATEUR ───────────────────────────
    /**
     * Invalide tous les tokens associés à un utilisateur.
     * Utile pour nettoyer les anciens tokens lors d'une nouvelle demande.
     * 
     * @param userId l'ID de l'utilisateur
     */
    public void invalidateTokensForUser(int userId) {
        int count = 0;
        for (Iterator<Map.Entry<String, TokenInfo>> it = tokenStore.entrySet().iterator();
             it.hasNext(); ) {
            Map.Entry<String, TokenInfo> entry = it.next();
            if (entry.getValue().userId == userId) {
                it.remove();
                count++;
            }
        }
        if (count > 0) {
            logger.info(" {} token(s) supprimé(s) pour UserId: {}", count, userId);
        }
    }
    
    // ── VÉRIFIER L'EXISTENCE D'UN TOKEN VALIDE ───────────────────────────
    /**
     * Vérifie si un token existe et est valide (non expiré, non utilisé).
     * N'invalide pas le token (contrairement à validateAndConsumeToken).
     * Utile pour les vérifications non-consommatrices.
     * 
     * @param token le token à vérifier
     * @return true si le token existe et est valide
     */
    public boolean isTokenValid(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        
        TokenInfo info = tokenStore.get(token);
        if (info == null) {
            return false;
        }
        
        if (info.used) {
            return false;
        }
        
        return System.currentTimeMillis() <= info.expirationTime;
    }
    
    // ── OBTENIR L'ID UTILISATEUR D'UN TOKEN (sans consommer) ─────────────
    /**
     * Récupère l'ID utilisateur associé à un token sans le consommer.
     * Utile pour les logs ou les vérifications.
     * 
     * @param token le token
     * @return l'ID utilisateur ou -1 si invalide
     */
    public int getUserIdFromToken(String token) {
        TokenInfo info = tokenStore.get(token);
        if (info == null || info.used || System.currentTimeMillis() > info.expirationTime) {
            return -1;
        }
        return info.userId;
    }
    
    // ── SUPPRIMER UN TOKEN ───────────────────────────────────────────────
    /**
     * Supprime un token du store (utile si on veut l'invalider explicitement).
     * 
     * @param token le token à supprimer
     */
    public void removeToken(String token) {
        if (tokenStore.remove(token) != null) {
            logger.info(" Token supprimé");
        }
    }
    
    // ── NETTOYAGE AUTOMATIQUE ────────────────────────────────────────────
    /**
     * Démarre un thread de nettoyage qui supprime les tokens expirés
     * toutes les 2 minutes pour éviter une accumulation en mémoire.
     */
    private void startCleanupThread() {
        Thread cleanupThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(2 * 60 * 1000); // Nettoyage chaque 2 minutes
                    long now = System.currentTimeMillis();
                    int removed = 0;
                    
                    for (Iterator<Map.Entry<String, TokenInfo>> it = tokenStore.entrySet().iterator();
                         it.hasNext(); ) {
                        Map.Entry<String, TokenInfo> entry = it.next();
                        if (now > entry.getValue().expirationTime || entry.getValue().used) {
                            it.remove();
                            removed++;
                        }
                    }
                    
                    if (removed > 0) {
                        logger.debug(" Cleanup: {} token(s) expirés/utilisés supprimés", removed);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        
        cleanupThread.setDaemon(true);
        cleanupThread.setName("ResetTokenStore-Cleanup");
        cleanupThread.start();
    }
    
    // ── UTILITAIRES ──────────────────────────────────────────────────────
    /**
     * Convertit un tableau de bytes en string hexadécimal.
     * 
     * @param bytes le tableau de bytes
     * @return la représentation hexadécimale
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
    
    // ── STATS (pour les tests/monitoring) ────────────────────────────────
    /**
     * Retourne le nombre de tokens actuellement en mémoire.
     * Utile pour le monitoring et le debugging.
     * 
     * @return nombre de tokens actifs
     */
    public int getTokenCount() {
        return tokenStore.size();
    }
    
    /**
     * Vide tous les tokens du store (pour tests).
     */
    public void clearAll() {
        int count = tokenStore.size();
        tokenStore.clear();
        if (count > 0) {
            logger.warn(" Tous les {} tokens ont été supprimés", count);
        }
    }
}
