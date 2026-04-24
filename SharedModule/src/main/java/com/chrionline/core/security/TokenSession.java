package com.chrionline.core.security;

import com.chrionline.shared.models.Utilisateur;

import java.io.Serializable;

/**
 * Représente une session de token avec les métadonnées de sécurité.
 *
 * Fonctionnalités:
 * - Stockage de l'utilisateur
 * - Timestamp de création et d'expiration
 * - Binding à une adresse IP
 * - Révocation manuelle
 *
 * CORRECTIONS APPORTÉES:
 * - Ajout de la constante TTL_DEFAULT_MINUTES pour centraliser la valeur de 2h
 * - Validation stricte des paramètres dans le constructeur (null check)
 */
public class TokenSession implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Durée de vie par défaut d'un token : 2 heures */
    public static final long TTL_DEFAULT_MINUTES = 120;

    private final String token;
    private final Utilisateur utilisateur;
    private final long createdAt;
    private final long expiresAt;
    private final String boundIp;
    private boolean revoked;

    /**
     * Crée une nouvelle session de token.
     *
     * @param token      le token UUID — ne peut pas être null
     * @param utilisateur l'utilisateur associé — ne peut pas être null
     * @param boundIp    l'adresse IP du client — ne peut pas être null
     * @param ttlMinutes temps de vie en minutes — doit être > 0
     *
     * @throws IllegalArgumentException si un paramètre est invalide
     */
    public TokenSession(String token, Utilisateur utilisateur, String boundIp, long ttlMinutes) {
        // CORRECTION: Validation des paramètres dès la construction
        // (avant, un token null ou un utilisateur null pouvait être stocké silencieusement)
        if (token == null || token.isBlank())
            throw new IllegalArgumentException("Le token ne peut pas être null ou vide");
        if (utilisateur == null)
            throw new IllegalArgumentException("L'utilisateur ne peut pas être null");
        if (boundIp == null || boundIp.isBlank())
            throw new IllegalArgumentException("L'IP de binding ne peut pas être null ou vide");
        if (ttlMinutes <= 0)
            throw new IllegalArgumentException("Le TTL doit être positif");

        this.token       = token;
        this.utilisateur = utilisateur;
        this.boundIp     = boundIp;
        this.createdAt   = System.currentTimeMillis();
        this.expiresAt   = this.createdAt + (ttlMinutes * 60 * 1000);
        this.revoked     = false;
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public String getToken()           { return token; }
    public Utilisateur getUtilisateur(){ return utilisateur; }
    public long getCreatedAt()         { return createdAt; }
    public long getExpiresAt()         { return expiresAt; }
    public String getBoundIp()         { return boundIp; }
    public boolean isRevoked()         { return revoked; }

    // ─── State Checks ─────────────────────────────────────────────────────────

    /**
     * Vérifie si le token a expiré.
     *
     * @return true si le token a dépassé son temps de vie
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

    /**
     * Vérifie si le token est valide (pas expiré ET pas révoqué).
     *
     * @return true si le token peut encore être utilisé
     */
    public boolean isValid() {
        return !isExpired() && !revoked;
    }

    /**
     * Vérifie si l'IP du client correspond au binding IP du token.
     *
     * @param clientIp l'adresse IP du client actuel
     * @return true si l'IP correspond
     */
    public boolean isIpBindingValid(String clientIp) {
        if (boundIp == null || clientIp == null) return false;
        return boundIp.equalsIgnoreCase(clientIp);
    }

    /**
     * Révoque manuellement le token (logout, changement de mot de passe, etc).
     */
    public void revoke() {
        this.revoked = true;
    }

    // ─── Information ──────────────────────────────────────────────────────────

    /**
     * @return temps restant avant expiration en secondes (0 si déjà expiré)
     */
    public long getRemainingSeconds() {
        long remaining = expiresAt - System.currentTimeMillis();
        return Math.max(0, remaining / 1000);
    }

    /**
     * @return raison de l'invalidité du token, ou null si le token est valide
     */
    public String getInvalidityReason() {
        if (revoked)    return "Token révoqué (logout ou changement de mot de passe)";
        if (isExpired()) return "Token expiré (validité dépassée)";
        return null;
    }

    @Override
    public String toString() {
        return "TokenSession{" +
                "token='"     + token.substring(0, 8) + "...'" +
                ", user='"    + utilisateur.getEmail() + '\'' +
                ", ip='"      + boundIp + '\'' +
                ", expiresIn=" + getRemainingSeconds() + "s" +
                ", valid="    + isValid() +
                '}';
    }
}