package com.chrionline.core.utils;

import com.chrionline.core.annotations.RequiresRole;
import com.chrionline.core.enums.UserRole;
import com.chrionline.core.security.TokenManager;
import com.chrionline.core.network.protocol.AppRequest;
import com.chrionline.shared.models.Utilisateur;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Service centralisé pour les vérifications d'autorisation (RBAC).
 * 
 * Fournit des méthodes pour :
 * - Récupérer l'utilisateur connecté depuis une requête
 * - Vérifier si un utilisateur a accès à une action
 * - Valider les annotations @RequiresRole sur les méthodes
 * - Gérer le contexte d'authentification
 * 
 * Intègre TokenManager pour la gestion du cycle de vie des tokens
 * (expiration 2h, IP binding, révocation).
 */
public class AuthorizationService {
    private static final Logger logger = LoggerFactory.getLogger(AuthorizationService.class);

    /**
     * Récupère l'utilisateur connecté associé à une requête.
     * Extrait le token du header "authToken" et cherche l'utilisateur correspondant.
     * Valide aussi l'IP du client si le header "client-address" est présent.
     * 
     * @param request la requête AppRequest contenant potentiellement le token
     * @return l'Utilisateur connecté, ou null si non authentifié
     */
    public static Utilisateur getAuthenticatedUser(AppRequest request) {
        if (request == null) {
            return null;
        }

        // Récupère le token depuis les headers ou le champ dédié
        String authToken = request.getHeader("authToken");
        if (authToken == null || authToken.isBlank()) {
            authToken = request.getAuthToken();
        }

        if (authToken == null || authToken.isBlank()) {
            return null;
        }

        // Récupère l'IP du client si disponible
        String clientIp = request.getHeader("client-address");

        // Delegue au gestionnaire de sessions/tokens (TokenManager si initié, sinon AuthContext)
        return AuthContext.getUserFromToken(authToken, clientIp);
    }

    /**
     * Vérifie si une méthode est protégée par @RequiresRole.
     * 
     * @param method la méthode à vérifier
     * @return true si la méthode a l'annotation @RequiresRole
     */
    public static boolean isRoleProtected(Method method) {
        return method != null && method.isAnnotationPresent(RequiresRole.class);
    }

    /**
     * Valide si l'utilisateur a accès à une méthode protégée par @RequiresRole.
     * 
     * @param user l'utilisateur (peut être null)
     * @param method la méthode annotée avec @RequiresRole
     * @return raison d'accès refusé, ou null si accès autorisé
     */
    public static String validateAccess(Utilisateur user, Method method) {
        if (method == null) {
            return null; // Pas de restriction
        }

        RequiresRole annotation = method.getAnnotation(RequiresRole.class);
        if (annotation == null) {
            return null; // Pas de restriction
        }

        // Vérification de l'authentification
        if (annotation.requiresAuthentication() && user == null) {
            logger.warn("Accès refusé : utilisateur non authentifié pour {}", method.getName());
            return "Authentification requise pour accéder à cette ressource";
        }

        // Pas de vérification de rôle si utilisateur null ET pas d'authentification requise
        if (user == null) {
            return null;
        }

        // Vérification du rôle
        UserRole userRole = UserRole.fromString(user.getRole());
        UserRole[] requiredRoles = annotation.value();

        if (requiredRoles == null || requiredRoles.length == 0) {
            requiredRoles = new UserRole[]{UserRole.ADMIN}; // Par défaut ADMIN
        }

        boolean hasPermission = Arrays.stream(requiredRoles)
                .anyMatch(role -> userRole.hasPermission(role));

        if (!hasPermission) {
            logger.warn(
                    "Accès refusé : rôle '{}' ne peut pas accéder à {} (requis: {})",
                    userRole, method.getName(), Arrays.toString(requiredRoles)
            );
            String roleList = String.join(", ", Arrays.stream(requiredRoles)
                    .map(UserRole::toString).toArray(String[]::new));
            return "Accès refusé. Rôles requis : " + roleList;
        }

        logger.debug("Accès autorisé : {} accède à {} (rôle: {})", 
                user.getEmail(), method.getName(), userRole);
        return null; // Accès autorisé
    }

    /**
     * Valide l'accès de manière simplifiée pour une requête et une méthode.
     * 
     * @param request la requête contenant potentiellement l'authentification
     * @param method la méthode à vérifier
     * @return raison d'accès refusé, ou null si accès autorisé
     */
    public static String validateAccess(AppRequest request, Method method) {
        Utilisateur user = getAuthenticatedUser(request);
        return validateAccess(user, method);
    }

    /**
     * Classe interne pour gérer le contexte d'authentification.
     * Utilise TokenManager pour la gestion du cycle de vie des tokens.
     * 
     * INITIALISATION:
     * - AuthController doit appeler AuthContext.initializeTokenManager(tokenManager)
     *   au démarrage du serveur pour activer la gestion TokenManager
     * - Les sessions sont alors validées avec expiration (2h), IP binding, révocation
     * 
     * RÉTROCOMPATIBILITÉ:
     * - Si TokenManager n'est pas initialisé, utilise une map legacy (AuthContext.sessionMap)
     */
    public static class AuthContext {
        private static volatile java.util.Map<String, Utilisateur> sessionMap = null;
        private static volatile TokenManager tokenManager = null;

        /**
         * Initialise le contexte d'authentification avec TokenManager.
         * À appeler depuis AuthController au démarrage du serveur.
         * 
         * @param tm le gestionnaire de tokens singleton
         */
        public static void initializeTokenManager(TokenManager tm) {
            AuthContext.tokenManager = tm;
            logger.info("✅ TokenManager initialisé dans AuthContext");
        }

        /**
         * Initialise la map de sessions legacy (rétrocompatibilité).
         * À appeler depuis AuthController si TokenManager n'est pas utilisé.
         * 
         * @param sessions la map token -> Utilisateur
         */
        public static void initialize(java.util.Map<String, Utilisateur> sessions) {
            AuthContext.sessionMap = sessions;
        }

        /**
         * Récupère un utilisateur à partir de son token et son IP.
         * 
         * PRIORITY:
         * 1. Si TokenManager est initialisé → utilise getValidSession(token, clientIp)
         *    avec validation d'expiration, IP binding, révocation
         * 2. Sinon, utilise la map legacy sessionMap (pas de validation IP/expiration)
         * 
         * @param token le token de session
         * @param clientIp l'IP du client (optionnel, ignoré si TokenManager absent)
         * @return l'Utilisateur, ou null si pas trouvé/invalide
         */
        public static Utilisateur getUserFromToken(String token, String clientIp) {
            if (token == null || token.isBlank()) {
                return null;
            }

            // PRIORITY 1: TokenManager (si initialisé)
            if (tokenManager != null) {
                try {
                    // Si clientIp non disponible, utilise getSessionWithoutIpCheck
                    // (par ex. depuis RequestDispatcher sans accès au header)
                    if (clientIp == null || clientIp.isBlank()) {
                        var session = tokenManager.getSessionWithoutIpCheck(token);
                        return session != null ? session.getUtilisateur() : null;
                    }

                    // Validation complète avec IP binding
                    return tokenManager.getUser(token, clientIp);
                } catch (Exception e) {
                    logger.error("❌ Erreur lors de la validation TokenManager: {}", e.getMessage(), e);
                    return null;
                }
            }

            // PRIORITY 2: Map legacy (rétrocompatibilité)
            if (sessionMap != null && !token.isBlank()) {
                return sessionMap.get(token);
            }

            return null;
        }

        /**
         * Récupère un utilisateur à partir de son token (sans validation IP).
         * Utilisé quand l'IP n'est pas disponible (par ex. depuis RequestDispatcher).
         * 
         * @param token le token de session
         * @return l'Utilisateur, ou null si pas trouvé
         */
        public static Utilisateur getUserFromToken(String token) {
            return getUserFromToken(token, null);
        }

        /**
         * Ajoute une session (appelé par login avec legacy sessions).
         * DEPRECATED: utiliser TokenManager.createToken() au lieu de ceci.
         * 
         * @param token le token de session
         * @param user l'utilisateur
         */
        @Deprecated
        public static void addSession(String token, Utilisateur user) {
            if (sessionMap != null) {
                sessionMap.put(token, user);
            }
        }

        /**
         * Supprime une session (appelé par logout avec legacy sessions).
         * DEPRECATED: utiliser TokenManager.revokeToken() au lieu de ceci.
         * 
         * @param token le token de session
         */
        @Deprecated
        public static void removeSession(String token) {
            if (sessionMap != null) {
                sessionMap.remove(token);
            }
        }

        /**
         * Vide toutes les sessions (rarement utilisé, pour tests).
         * DEPRECATED: utiliser TokenManager.forceCleanup() au lieu de ceci.
         */
        @Deprecated
        public static void clearSessions() {
            if (sessionMap != null) {
                sessionMap.clear();
            }
        }
    }
}
