package com.chrionline.core.security;

import com.chrionline.core.utils.AuthorizationService;
import com.chrionline.core.network.protocol.AppRequest;
import com.chrionline.shared.models.Utilisateur;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validateur d'ownership — Prévention d'escalade de privilèges.
 * 
 * Assure que les utilisateurs ne peuvent accéder/modifier que LEURS propres données.
 * 
 * Responsabilités:
 * - Vérifier que le userId en paramètre correspond à l'utilisateur authentifié
 * - Détecter et logger les tentatives d'escalade
 * - Rejeter les requêtes malveillantes avec code d'erreur 403
 */
public class OwnershipValidator {
    private static final Logger logger = LoggerFactory.getLogger(OwnershipValidator.class);

    /**
     * Valide que l'utilisateur authentifié possède la ressource spécifiée par userId.
     * 
     * Cas d'usage:
     * - Vérifier qu'un utilisateur ne peut voir que SON panier
     * - Vérifier qu'un utilisateur ne peut modifier que SES adresses
     * - Vérifier qu'un utilisateur ne peut passer de commande que pour LUI-MÊME
     * 
     * @param request la requête contenant le token d'authentification
     * @param resourceUserId l'ID de l'utilisateur propriétaire de la ressource
     * @param resourceType type de ressource (pour les logs: "panier", "adresse", etc.)
     * @return raison de rejet si validation échouée, null si validé
     */
    public static String validateOwnership(AppRequest request, int resourceUserId, String resourceType) {
        if (request == null) {
            logger.warn("Validation d'ownership échouée: request null pour {}", resourceType);
            return "Requête invalide";
        }

        // Récupère l'utilisateur authentifié
        Utilisateur authenticatedUser = AuthorizationService.getAuthenticatedUser(request);
        if (authenticatedUser == null) {
            logger.warn("Validation d'ownership échouée: utilisateur non authentifié pour {}", resourceType);
            return "Authentification requise";
        }

        int authenticatedUserId = authenticatedUser.getId();

        // VÉRIFICATION CRITIQUE: L'ID doit correspondre
        if (authenticatedUserId != resourceUserId) {
            logger.warn(
                    "TENTATIVE D'ESCALADE DE PRIVILÈGES DÉTECTÉE! " +
                    "| Utilisateur ID: {} (email: {}) " +
                    "| Tentative d'accès au {} ID: {} " +
                    "| ACCÈS REFUSÉ",
                    authenticatedUserId,
                    authenticatedUser.getEmail(),
                    resourceType,
                    resourceUserId
            );
            return "Accès refusé: vous ne pouvez accéder que à vos propres " + resourceType;
        }

        logger.debug(
                "Ownership validé | User: {} (ID: {}) | {}: {}",
                authenticatedUser.getEmail(),
                authenticatedUserId,
                resourceType,
                resourceUserId
        );

        return null; // Validation réussie
    }

    /**
     * Valide l'ownership et retourne l'utilisateur si validé.
     * 
     * @param request la requête contenant le token
     * @param resourceUserId l'ID de l'utilisateur propriétaire de la ressource
     * @param resourceType type de ressource (pour les logs)
     * @return l'utilisateur authentifié si propriétaire, null sinon
     */
    public static Utilisateur validateOwnershipAndGetUser(AppRequest request, int resourceUserId, String resourceType) {
        String error = validateOwnership(request, resourceUserId, resourceType);
        if (error != null) {
            return null;
        }
        return AuthorizationService.getAuthenticatedUser(request);
    }

    /**
     * Valide que l'utilisateur authentifié est ADMIN ou PROPRIÉTAIRE.
     * Utile pour les opérations sensibles (suppression, modifications importantes).
     * 
     * @param request la requête
     * @param resourceUserId l'ID de l'utilisateur propriétaire
     * @param resourceType type de ressource (pour les logs)
     * @return raison de rejet si validation échouée, null si validé
     */
    public static String validateOwnershipOrAdmin(AppRequest request, int resourceUserId, String resourceType) {
        if (request == null) {
            return "Requête invalide";
        }

        Utilisateur authenticatedUser = AuthorizationService.getAuthenticatedUser(request);
        if (authenticatedUser == null) {
            return "Authentification requise";
        }

        int authenticatedUserId = authenticatedUser.getId();

        // Les ADMIN peuvent tout faire
        if ("admin".equals(authenticatedUser.getRole())) {
            logger.debug("Admin override: admin ID: {} accède à {} ID: {}",
                    authenticatedUserId, resourceType, resourceUserId);
            return null;
        }

        // Les clients doivent être propriétaire
        if (authenticatedUserId != resourceUserId) {
            logger.warn(
                    "TENTATIVE D'ESCALADE (ou admin override manqué) | " +
                    "User: {} (ID: {}) | {}: {} | REFUSÉ",
                    authenticatedUser.getEmail(),
                    authenticatedUserId,
                    resourceType,
                    resourceUserId
            );
            return "Accès refusé: vous ne pouvez accéder que à vos propres " + resourceType;
        }

        return null; // Validation réussie
    }

    /**
     * Valide plusieurs ressources appartiennent au même utilisateur.
     * Utile pour les opérations batch (supprimer plusieurs adresses, etc.).
     * 
     * @param request la requête
     * @param resourceUserIds les IDs d'utilisateur des ressources
     * @param resourceType type de ressource (pour les logs)
     * @return raison de rejet si validation échouée, null si validé
     */
    public static String validateMultipleOwnership(AppRequest request, int[] resourceUserIds, String resourceType) {
        if (request == null) {
            return "Requête invalide";
        }

        Utilisateur authenticatedUser = AuthorizationService.getAuthenticatedUser(request);
        if (authenticatedUser == null) {
            return "Authentification requise";
        }

        int authenticatedUserId = authenticatedUser.getId();

        // Vérifie que TOUTES les ressources appartiennent à l'utilisateur
        for (int userId : resourceUserIds) {
            if (userId != authenticatedUserId) {
                logger.warn(
                        "TENTATIVE D'ESCALADE (batch) | User: {} (ID: {}) | " +
                        "Tentative sur {} ID: {} | REFUSÉ",
                        authenticatedUser.getEmail(),
                        authenticatedUserId,
                        resourceType,
                        userId
                );
                return "Accès refusé: une ou plusieurs " + resourceType + " ne vous appartiennent pas";
            }
        }

        logger.debug("Ownership batch validé | User: {} | {} ressource(s)",
                authenticatedUser.getEmail(), resourceUserIds.length);

        return null; // Validation réussie
    }

    /**
     * Extrait le userId de la requête (souvent un paramètre comme ?userId=123).
     * Utile pour éviter les répétitions dans les contrôleurs.
     * 
     * @param request la requête
     * @param paramName nom du paramètre (ex: "userId", "id_utilisateur")
     * @return l'ID utilisateur, ou -1 si invalide
     */
    public static int extractUserIdFromRequest(AppRequest request, String paramName) {
        try {
            Integer userId = request.getInt(paramName);
            return userId != null ? userId : -1;
        } catch (Exception e) {
            logger.warn("Impossible d'extraire {} de la requête", paramName);
            return -1;
        }
    }

    /**
     * Méthode de convenience: valide et rejette en un appel.
     * Retourne un message d'erreur prêt pour AppResponse.forbidden().
     * 
     * @param request la requête
     * @param resourceUserId l'ID de l'utilisateur propriétaire
     * @param resourceType type de ressource
     * @return message d'erreur si invalide, null si OK
     */
    public static String checkOwnership(AppRequest request, int resourceUserId, String resourceType) {
        return validateOwnership(request, resourceUserId, resourceType);
    }
}
