package com.chrionline.server.services;


import com.chrionline.core.config.ServerConfig;
import com.chrionline.server.repositories.UtilisateurRepository;
import com.chrionline.shared.models.Utilisateur;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Vérifie le code 2FA pendant le flow de connexion.
 * S'intercale entre la validation du mot de passe et la délivrance du token final.
 */
public class TwoFactorVerifier {

    private static final Logger logger = LoggerFactory.getLogger(TwoFactorVerifier.class);
    private final TwoFactorService twoFactorService;

    public TwoFactorVerifier() {
        this.twoFactorService = new TwoFactorService();
    }

    /**
     * Vérifie le code soumis après login.
     * @param tempToken token temporaire reçu après password OK
     * @param code      code à 6 chiffres saisi par l'utilisateur
     * @return l'utilisateur si valide, null sinon
     */
    public Utilisateur verify(String tempToken, String code) {
        if (tempToken == null || code == null) {
            logger.warn("2FA: tempToken ou code manquant.");
            return null;
        }

        Integer userId = twoFactorService.getUserIdFromPendingToken(tempToken);
        if (userId == null) {
            logger.warn("2FA: token temporaire invalide ou expiré.");
            return null;
        }

        Utilisateur u = ServerConfig.getRepo(UtilisateurRepository.class).getById(userId);
        if (u == null) return null;

        if (!twoFactorService.validateCode(u.getTwoFactorSecret(), code)) {
            logger.warn("2FA: code invalide pour userId={}", userId);
            return null;
        }

        // ✅ Code valide → on supprime le token temporaire
        twoFactorService.removePendingToken(tempToken);
        logger.info("2FA validé pour userId={}", userId);
        return u;
    }
}
