package com.chrionline.network;

import com.chrionline.core.config.ServerConfig;
import com.chrionline.core.interfaces.IController;
import com.chrionline.core.utils.AuthorizationService;
import com.chrionline.core.network.protocol.AppRequest;
import com.chrionline.core.network.protocol.AppResponse;
import com.chrionline.network.protocol.RequestParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Dispatcher central pour le routage des requêtes vers les contrôleurs.
 * 
 * Fonctionnalités:
 * - Validation des requêtes
 * - Résolution du contrôleur et de la méthode
 * - Vérification des permissions RBAC (@RequiresRole)
 * - Gestion des erreurs et exceptions
 */
public class RequestDispatcher {
    private static final Logger logger = LoggerFactory.getLogger(RequestDispatcher.class);

    /**
     * Envoie un message en format simple (legacy).
     * NOTA: Ce mode ne supporte pas la vérification RBAC.
     * 
     * @param message format: "ControllerName#action:payload"
     * @return réponse JSON
     */
    public static String dispatch(String message) {
        try {
            String[] parts = message.split("#", 2);

            if (parts.length < 2) {
                return AppResponse.badRequest("ERROR: Invalid message format. Expected: Controller#action:payload");
            }

            String controllerName = parts[0];
            String[] actionPayload = parts[1].split(":", 2);

            String action = actionPayload[0];
            String payload = actionPayload.length > 1 ? actionPayload[1] : "";

            IController controller = ServerConfig.getController(controllerName);

            if(controller == null) {
                return AppResponse.error("Controller '" + controllerName + "' not found");
            }

            Method method = controller.getClass()
                    .getMethod(action.toLowerCase(), String.class);

            return (String) method.invoke(controller, payload);

        } catch (NoSuchMethodException e) {
            return AppResponse.error("Action not found: " + e.getMessage());
        } catch (Exception e) {
            return AppResponse.error(e.getMessage());
        }
    }

    /**
     * Envoie une requête AppRequest complète avec support RBAC.
     * 
     * Processus:
     * 1. Valide la structure de la requête
     * 2. Récupère le contrôleur
     * 3. Trouve la méthode correspondante
     * 4. **Vérifie les permissions RBAC (@RequiresRole)**
     * 5. Invoque la méthode si autorisée
     * 
     * @param request la requête AppRequest contenant controller, action, payload, authToken
     * @return réponse JSON (succès, erreur, ou "Accès refusé")
     */
    public static String dispatch(AppRequest request) {
        try {
            // 1. Validation de la requête
            RequestParser.validate(request);

            // 2. Récupération du contrôleur
            IController controller = ServerConfig.getController(request.getController());
            if (controller == null) {
                logger.warn("Contrôleur non trouvé : {}", request.getController());
                return AppResponse.error("Controller '" + request.getController() + "' not found");
            }

            // 3. Résolution de la méthode
            Method method = findActionMethod(controller, request.getAction());

            // 4.  VÉRIFICATION RBAC - Contrôle d'accès basé sur les rôles 
            String authorizationError = AuthorizationService.validateAccess(request, method);
            if (authorizationError != null) {
                logger.warn(
                        "Accès REFUSÉ: {} → {}.{} | Raison: {}",
                        request.getHeader("authToken") != null ? "Authenticated" : "Anonymous",
                        request.getController(),
                        request.getAction(),
                        authorizationError
                );
                return AppResponse.forbidden(authorizationError);
            }

            // 5. Invocation de la méthode (si autorisée)
            logger.debug("Accès autorisé: {} → {}.{}", 
                    request.getHeader("authToken") != null ? "Authenticated" : "Anonymous",
                    request.getController(),
                    request.getAction());

            return (String) method.invoke(controller, request);

        } catch (NoSuchMethodException e) {
            logger.error("Méthode non trouvée : {}.{}", 
                    request.getController(), request.getAction());
            return AppResponse.error("Action '" + request.getAction() + "' not found in controller '" +
                    request.getController() + "'");
        } catch (Exception e) {
            logger.error("Erreur lors du dispatch", e);
            return AppResponse.error(e.getMessage());
        }
    }

    /**
     * Trouve la méthode correspondant à une action.
     * Recherche d'abord une méthode prenant AppRequest, puis String.
     * 
     * @param controller le contrôleur
     * @param action le nom de l'action
     * @return la méthode trouvée
     * @throws NoSuchMethodException si aucune méthode ne correspond
     */
    private static Method findActionMethod(IController controller, String action)
            throws NoSuchMethodException {
        try {
            return controller.getClass().getMethod(action, AppRequest.class);
        } catch (NoSuchMethodException e) {
            // Fallback sur la signature String (legacy)
            return controller.getClass().getMethod(action, String.class);
        }
    }
}
